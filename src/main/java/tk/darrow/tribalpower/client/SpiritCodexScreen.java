package tk.darrow.tribalpower.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import tk.darrow.tribalpower.echo.LatticeRecipe;
import tk.darrow.tribalpower.guide.CodexEntries;
import tk.darrow.tribalpower.guide.CodexEntries.Entry;

import java.nio.file.Files;
import java.util.*;

/** Native, client-only illustrated reference. No optional book or recipe mod is required. */
public final class SpiritCodexScreen extends Screen {
    private static final int INK=0xFF101C27, TEAL=0xFF74DBCB, GOLD=0xFFE4C18A, PAPER=0xFFE4E5DA;
    private final Set<String> bookmarks = new HashSet<>();
    private final Deque<String> history = new ArrayDeque<>();
    private final List<Hit> hits = new ArrayList<>();
    private final List<String> categories = new ArrayList<>(List.of("Contents", "Bookmarks", "Recipe index"));
    private String query="", category="Contents", selected="chapter_1", recipeItem="";
    private boolean spoilers, motion=true, showUses;
    private int left, top, bookWidth, bookHeight, sidebar, contentX, contentWidth, listPage, scroll, maxScroll, recipePage;
    private EditBox search;
    private List<RecipeHolder<?>> recipes=List.of();
    private record Hit(int x,int y,ItemStack item) {}

    public SpiritCodexScreen() {
        super(Component.literal("Spirit Codex"));
        CodexEntries.ALL.stream().map(Entry::category).distinct().forEach(categories::add);
        try {
            var file=Minecraft.getInstance().gameDirectory.toPath().resolve("config/tribalpower-codex-bookmarks.txt");
            if(Files.isRegularFile(file)) Files.readAllLines(file).stream().limit(128)
                    .filter(id->CodexEntries.ALL.stream().anyMatch(e->e.id().equals(id))).forEach(bookmarks::add);
        } catch(java.io.IOException ignored) { }
    }

    private Button button(String text,int x,int y,int w,java.util.function.Consumer<Button> action) {
        return addRenderableWidget(new Button(x,y,w,18,Component.literal(text),action::accept,narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
                boolean highlighted=isHoveredOrFocused();
                g.fillGradient(getX(),getY(),getX()+getWidth(),getY()+getHeight(),highlighted?0xFF345A5D:0xFF223A45,0xFF142530);
                g.renderOutline(getX(),getY(),getWidth(),getHeight(),highlighted?GOLD:0xFF42636A);
                g.drawCenteredString(font,getMessage(),getX()+getWidth()/2,getY()+5,active?(highlighted?GOLD:PAPER):0xFF667A80);
            }
        });
    }

    @Override protected void init() {
        bookWidth=Math.min(width-12,720); bookHeight=Math.min(height-12,430);
        left=(width-bookWidth)/2; top=(height-bookHeight)/2;
        sidebar=Math.min(166,bookWidth/3);contentX=left+sidebar+18;contentWidth=bookWidth-sidebar-32;
        search=addRenderableWidget(new EditBox(font,left+10,top+36,bookWidth-20,18,Component.literal("Search the Codex")));
        search.setMaxLength(96);search.setHint(Component.literal("Search teachings and items..."));search.setValue(query);
        search.setResponder(value->{query=value;listPage=0; rebuildList();});
        button(category,left+10,top+60,sidebar-8,b->{
            List<String> visible=categories.stream().filter(c->spoilers||c.equals("Contents")||c.equals("Bookmarks")||CodexEntries.ALL.stream().anyMatch(e->e.category().equals(c)&&!e.spoiler())).toList();
            String next=visible.get((visible.indexOf(category)+1)%visible.size());
            category=next;listPage=0;rebuildWidgets();
        });
        button("<",left+10,top+bookHeight-49,28,b->{listPage=Math.max(0,listPage-1);rebuildWidgets();});
        button(">",left+42,top+bookHeight-49,28,b->{listPage++;rebuildWidgets();});
        button("Home",left+76,top+bookHeight-49,sidebar-74,b->{category="Contents";query="";listPage=0;openEntry("chapter_1");});
        button("Back",left+10,top+bookHeight-25,46,b->{if(history.isEmpty())onClose();else navigate(history.pop());});
        button("Close",left+60,top+bookHeight-25,46,b->onClose());
        button(spoilers?"Hide spoilers":"Spoilers...",left+110,top+bookHeight-25,86,b->{
            if(spoilers){spoilers=false;category="Contents";query="";listPage=0;history.clear();navigate("chapter_1");}
            else confirmSpoilers(()->{});
        });
        button(motion?"Motion: on":"Motion: off",left+bookWidth-98,top+bookHeight-25,88,b->{motion=!motion;rebuildWidgets();});
        button(recipeItem.isEmpty()?(bookmarks.contains(selected)?"Unmark":"Bookmark"):(showUses?"Show recipes":"Show uses"),contentX,top+60,76,b->{
            if(recipeItem.isEmpty()){if(!bookmarks.remove(selected))bookmarks.add(selected);saveBookmarks();}
            else {showUses=!showUses;recipePage=0;scroll=0;findRecipes();}
            rebuildWidgets();
        });
        if(recipeItem.isEmpty())button("Recipes",contentX+80,top+60,60,b->{Entry e=current(); if(e!=null)openRecipes("tribalpower:"+e.icon());});
        else {
            button("<",contentX+80,top+60,28,b->{recipePage--;scroll=0;});
            button(">",contentX+112,top+60,28,b->{recipePage++;scroll=0;});
        }
        rebuildList();
    }

    private final List<Button> listButtons=new ArrayList<>();
    private void rebuildList() {
        for(Button b:listButtons) removeWidget(b);listButtons.clear();
        List<String[]> rows=new ArrayList<>();String needle=query.toLowerCase(Locale.ROOT);
        if(category.equals("Recipe index")&&spoilers) {
            BuiltInRegistries.ITEM.entrySet().stream().filter(e->e.getKey().location().getNamespace().equals("tribalpower"))
                    .sorted(Comparator.comparing(e->e.getValue().getDescription().getString()))
                    .forEach(e->{String name=e.getValue().getDescription().getString();if(name.toLowerCase(Locale.ROOT).contains(needle))rows.add(new String[]{"item:"+e.getKey().location(),name});});
        } else {
            for(Entry e:CodexEntries.ALL) if((spoilers||!e.spoiler())
                    && (category.equals("Contents")||category.equals(e.category())||(category.equals("Bookmarks")&&bookmarks.contains(e.id())))
                    &&(e.title()+" "+e.text()).toLowerCase(Locale.ROOT).contains(needle)) rows.add(new String[]{e.id(),e.title()});
        }
        int count=Math.max(1,(bookHeight-142)/20);listPage=Math.min(listPage,Math.max(0,(rows.size()-1)/count));
        for(int i=listPage*count;i<Math.min(rows.size(),(listPage+1)*count);i++) {
            String[] row=rows.get(i);Button b=button(font.plainSubstrByWidth(row[1],sidebar-20),left+10,top+84+(i%count)*20,sidebar-8,v->{
                if(row[0].startsWith("item:"))openRecipes(row[0].substring(5));else openEntry(row[0]);
            });listButtons.add(b);
        }
    }

    private void saveBookmarks() {
        try {var file=minecraft.gameDirectory.toPath().resolve("config/tribalpower-codex-bookmarks.txt");Files.createDirectories(file.getParent());Files.write(file,new TreeSet<>(bookmarks));}
        catch(java.io.IOException ignored) { }
    }
    private void confirmSpoilers(Runnable after) {
        minecraft.setScreen(new ConfirmScreen(yes->{if(yes){spoilers=true;after.run();}minecraft.setScreen(this);},
                Component.literal("Beyond the veil — spoilers"),Component.literal("The full wiki reveals creatures, materials, recipes and late-game discoveries. Reveal them for this reading session?"),
                Component.literal("Reveal knowledge"),Component.literal("Keep discovering")));
    }
    private Entry current(){return CodexEntries.ALL.stream().filter(e->e.id().equals(selected)).findFirst().orElse(CodexEntries.ALL.getFirst());}
    private void openEntry(String id) {
        Entry target=CodexEntries.ALL.stream().filter(e->e.id().equals(id)).findFirst().orElse(null);
        if(target==null)return;
        if(target.spoiler()&&!spoilers){confirmSpoilers(()->openEntry(id));return;}
        remember();navigate(id);
    }
    private void navigate(String id) {
        scroll=0;recipePage=0;showUses=false;
        if(id.startsWith("item:")){recipeItem=id.substring(5);findRecipes();}
        else {selected=id;recipeItem="";}
        rebuildWidgets();
    }
    private void openRecipes(String id) {
        // Full recipe graphs can expose progression even through an early ingredient's uses.
        if(!spoilers){confirmSpoilers(()->openRecipes(id));return;}
        remember();navigate("item:"+id);
    }
    private void remember(){if(history.size()>=128)history.removeLast();history.push(recipeItem.isEmpty()?selected:"item:"+recipeItem);}
    private void findRecipes() {
        if(minecraft.level==null){recipes=List.of();return;}
        recipes=minecraft.level.getRecipeManager().getRecipes().stream().filter(h->{
            ItemStack output=h.value().getResultItem(minecraft.level.registryAccess());
            return showUses?h.value().getIngredients().stream().anyMatch(i->i.test(stack(recipeItem)))
                    :!output.isEmpty()&&BuiltInRegistries.ITEM.getKey(output.getItem()).toString().equals(recipeItem);
        }).sorted(Comparator.comparing(h->h.id().toString())).toList();
    }
    private ItemStack stack(String id) {var key=ResourceLocation.tryParse(id.contains(":")?id:"tribalpower:"+id);return key==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.get(key));}
    private int paragraph(GuiGraphics g,String text,int y,int color) {
        for(FormattedCharSequence line:font.split(Component.literal(text),contentWidth-12)){g.drawString(font,line,contentX+6,y,color,false);y+=12;}
        return y+8;
    }
    private void item(GuiGraphics g,ItemStack item,int x,int y) {
        g.fill(x-1,y-1,x+17,y+17,0xFF23373F);g.renderItem(item,x,y);g.renderItemDecorations(font,item,x,y);
        if(y>=top+86&&y+16<=top+bookHeight-55)hits.add(new Hit(x,y,item));
    }

    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        renderBackground(g,mx,my,partial);
        var book=ResourceLocation.parse("tribalpower:textures/gui/codex/book.png");
        int seam=sidebar+12;
        g.blit(book,left,top,seam,bookHeight,0F,0F,480,1003,1568,1003);
        g.blit(book,left+seam,top,bookWidth-seam,bookHeight,480F,0F,1088,1003,1568,1003);
        g.drawString(font,"SPIRIT CODEX",left+26,top+12,GOLD,false);
        String mode=spoilers?"THE VEIL IS OPEN":"SPOILER-SAFE";
        g.drawString(font,mode,left+bookWidth-font.width(mode)-26,top+12,TEAL,false);
        if(listButtons.isEmpty())g.drawString(font,"No matching pages",left+12,top+87,0xFF9CB8B9,false);
        hits.clear();int start=top+88;
        g.enableScissor(contentX,start,contentX+contentWidth,top+bookHeight-55);
        int y=start-scroll;
        if(recipeItem.isEmpty()) {
            Entry e=current();
            y=paragraph(g,e.title(),y,GOLD);
            if(!e.picture().isEmpty()) {
                var pic=ResourceLocation.parse("tribalpower:textures/gui/codex/"+e.picture()+".png");
                int size=Math.min(bookHeight<300?64:112,contentWidth-12);int px=contentX+(contentWidth-size)/2;
                g.blit(pic,px,y,0,0,size,size,size,size);y+=size+8;
            } else if(e.id().equals("chapter_1")) {
                int size=Math.min(bookHeight<300?64:112,contentWidth-12);
                g.blit(ResourceLocation.parse("tribalpower:textures/gui/codex/cover.png"),contentX+(contentWidth-size)/2,y,0,0,size,size,size,size);y+=size+8;
            } else y=diagram(g,e,y);
            y=paragraph(g,e.text(),y,PAPER);
            y=paragraph(g,"RELATED TEACHINGS",y+6,TEAL);
            for(Entry other:CodexEntries.ALL) if(!other.id().equals(e.id())&&other.category().equals(e.category())&&(spoilers||!other.spoiler())) {
                g.drawString(font,"> "+font.plainSubstrByWidth(other.title(),contentWidth-22),contentX+6,y,GOLD,false);
                y+=16;
            }
        } else {
            ItemStack focus=stack(recipeItem);item(g,focus,contentX+6,y);y=paragraph(g,focus.getHoverName().getString()+(showUses?" — uses":""),y+24,GOLD);
            y=paragraph(g,"Click ingredients to follow their recipes. Left/right arrow keys change variants.",y,TEAL);
            if(recipes.isEmpty())y=paragraph(g,showUses?"No registered recipe consumes this item as an ingredient. It may have a use in the world instead.":"No crafting or processing recipe is registered for this item. It may come from exploration, harvesting, trading or loot. Consult the relevant teaching.",y,PAPER);
            else {
                Recipe<?> recipe=recipes.get(Math.floorMod(recipePage,recipes.size())).value();
                y=paragraph(g,"Recipe "+(Math.floorMod(recipePage,recipes.size())+1)+" / "+recipes.size()+"  [< / >]",y,GOLD);
                if(recipe instanceof LatticeRecipe lattice) {
                    y=paragraph(g,lattice.station().replace('_',' ')+" / "+lattice.attunement().getSerializedName(),y,TEAL);
                    y=paragraph(g,lattice.seconds()+" seconds | "+lattice.pulse()+" Pulse/s | "+(lattice.seconds()*lattice.pulse())+" total Pulse",y,PAPER);
                } else y=paragraph(g,recipe instanceof ShapedRecipe?"Shaped crafting":recipe instanceof ShapelessRecipe?"Shapeless crafting":BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()).toString(),y,TEAL);
                int columns=recipe instanceof ShapedRecipe shaped?shaped.getWidth():3;
                int rows=recipe instanceof ShapedRecipe shaped?shaped.getHeight():(recipe.getIngredients().size()+2)/3;
                int index=0;
                for(Ingredient ingredient:recipe.getIngredients()) {
                    ItemStack[] options=ingredient.getItems();
                    if(options.length>0)item(g,options[motion?(int)(System.currentTimeMillis()/1400%options.length):0],contentX+8+(index%columns)*20,y+(index/columns)*20);
                    index++;
                }
                item(g,recipe.getResultItem(minecraft.level.registryAccess()),contentX+Math.min(110,contentWidth-24),y+20);
                g.drawString(font,">",contentX+82,y+24,GOLD,false);y+=Math.max(rows*20,60)+12;
                y=paragraph(g,"Recipes reflect this world's active datapacks. Hover an item for its name; click it to explore further.",y,PAPER);
            }
        }
        maxScroll=Math.max(0,y+scroll-(top+bookHeight-55));
        g.disableScissor();
        if(maxScroll>0){int track=bookHeight-145;int thumb=Math.max(12,track*track/(track+maxScroll));int pos=(track-thumb)*Math.min(scroll,maxScroll)/maxScroll;g.fill(left+bookWidth-7,start+pos,left+bookWidth-4,start+pos+thumb,TEAL);}
        // Screen.render invokes renderBackground in 1.21; calling it here would blur the book a second time.
        for(var widget:renderables)widget.render(g,mx,my,partial);
        for(Hit h:hits)if(mx>=h.x&&mx<h.x+16&&my>=h.y&&my<h.y+16)g.renderTooltip(font,h.item,mx,my);
    }

    private int diagram(GuiGraphics g,Entry e,int y) {
        int x=contentX+8,span=contentWidth-32;double t=motion?System.currentTimeMillis()/650.0:0;
        g.fill(contentX+4,y,contentX+contentWidth-4,y+65,0xFF0C1922);
        boolean cargo=e.category().contains("transport"),work=e.category().equals("Workshops"),rites=e.category().contains("rites");
        String label=cargo?"LINK > TRANSFER > REST":work?"ATTUNE > PROCESS > COLLECT":rites?"CHARGE > CAST > RECOVER":"BEAT > GATHER > STORE";
        if(e.category().equals("Camp stewardship"))label=switch(e.id()) {
            case "camp_binding_effigy","camp_binding_ritual" -> "IMPRINT > AWAKEN > RENEW";
            case "camp_summoning_cradle" -> "BIND > SUMMON > RENEW";
            case "camp_grove_tender" -> "PLANT > GROW > HARVEST";
            case "camp_wayanchor" -> "SUPPLY > SUSTAIN > RELEASE";
            case "camp_hush_totem" -> "SUPPLY > WARD > REST";
            default -> "PLACE > CONNECT > ENJOY";
        };
        g.drawString(font,font.plainSubstrByWidth(label,contentWidth-16),x,y+6,TEAL,false);
        String first=cargo?"ancestral_cache":work?"minecraft:stone":rites?"pulse_cell":"drumheart";
        String last=cargo?"ancestral_cache":work?"echo_shard":rites?e.icon():"pulse_cell";
        if(e.category().equals("Camp stewardship")) {
            first=e.icon();last="pulse_cell";
            if(e.id().equals("camp_summoning_cradle")){first="binding_effigy";last="summoning_cradle";}
            if(e.id().equals("camp_binding_ritual")){first="ritual_brazier";last="binding_effigy";}
            if(e.id().equals("camp_grove_tender")){first="minecraft:wheat_seeds";last="minecraft:wheat";}
        }
        item(g,stack(first),x,y+29);item(g,stack(last),x+span-16,y+29);
        int lineStart=x+24,lineEnd=x+span-22;g.fill(lineStart,y+37,lineEnd,y+38,0xFF36555B);
        for(int n=0;n<4;n++){double phase=(t/4+n/4.0)%1;int dx=lineStart+(int)((lineEnd-lineStart)*phase);int dy=y+36+(int)(Math.sin(t+n)*3);g.fill(dx,dy,dx+3,dy+3,TEAL);}
        double progress=(t/6)%1;
        int movingX=lineStart+(int)((lineEnd-lineStart-16)*progress);
        String moving=cargo?"minecraft:wheat":work?(progress<0.6?"minecraft:stone":"echo_shard"):"spirit_shard";
        if(e.category().equals("Camp stewardship"))moving=e.id().equals("camp_grove_tender")?(progress<0.6?"minecraft:wheat_seeds":"minecraft:wheat"):e.id().contains("binding")||e.id().equals("camp_summoning_cradle")?"spiritweave":e.icon();
        g.renderItem(stack(moving),movingX,y+25);
        int meterY=y+53;g.fill(x,meterY,x+span,meterY+3,0xFF29484F);g.fill(x,meterY,x+(int)(span*progress),meterY+3,TEAL);
        return paragraph(g,"Illustrated example — use Motion to pause. Live costs and ingredients are shown in Recipes.",y+71,0xFF9CB8B9);
    }

    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(x>=contentX){scroll=Math.clamp(scroll-(int)(vertical*24),0,maxScroll);return true;}
        listPage=Math.max(0,listPage+(vertical<0?1:-1));rebuildWidgets();return true;
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0&&y>=top+88&&y<top+bookHeight-55) {
            for(Hit h:hits)if(x>=h.x&&x<h.x+16&&y>=h.y&&y<h.y+16){openRecipes(BuiltInRegistries.ITEM.getKey(h.item.getItem()).toString());return true;}
            if(recipeItem.isEmpty()&&x>=contentX&&x<contentX+contentWidth) {
                Entry e=current();int pos=top+88-scroll;
                pos+=font.split(Component.literal(e.title()),contentWidth-12).size()*12+8;
                pos+=(!e.picture().isEmpty()||e.id().equals("chapter_1"))?Math.min(bookHeight<300?64:112,contentWidth-12)+8:71+font.split(Component.literal("Illustrated example — use Motion to pause. Live costs and ingredients are shown in Recipes."),contentWidth-12).size()*12+8;
                pos+=font.split(Component.literal(e.text()),contentWidth-12).size()*12+8+6;
                pos+=font.split(Component.literal("RELATED TEACHINGS"),contentWidth-12).size()*12+8;
                for(Entry other:CodexEntries.ALL)if(!other.id().equals(e.id())&&other.category().equals(e.category())&&(spoilers||!other.spoiler())){if(y>=pos&&y<pos+16){openEntry(other.id());return true;}pos+=16;}
            }
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(!search.isFocused()&&(key==266||key==267)){scroll=Math.clamp(scroll+(key==266?-100:100),0,maxScroll);return true;}
        if(!search.isFocused()&&!recipeItem.isEmpty()&&(key==262||key==263)){recipePage+=key==262?1:-1;return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean isPauseScreen(){return false;}
    /** Opt-in development tour; never runs in a normal game. */
    /** Showcase driver hook ({@link ShowcaseVerification}): reveal spoilers and jump straight to an entry or item. */
    void showcaseOpen(String id) {
        if(!Boolean.getBoolean("tribalpower.showcaseVerification"))return;
        spoilers=true;scroll=0;recipePage=0;
        if(id.startsWith("item:"))openRecipes(id.substring(5));else openEntry(id);
    }
    void verificationStage(int stage) {
        if(!Boolean.getBoolean("tribalpower.codexVerification"))return;
        if(stage==1)openEntry("chapter_4");
        if(stage==2)confirmSpoilers(()->openEntry("dawn_stag"));
        if(stage==4){category="Bestiary & skies";query="fox";openEntry("lantern_fox");}
        if(stage==5){spoilers=false;category="Contents";query="";history.clear();navigate("chapter_1");}
        if(stage==6){spoilers=true;openRecipes("tribalpower:drumheart");}
        if(stage==7)openRecipes("tribalpower:echo_shard");
        if(stage==8){recipePage=1;scroll=0;}
        if(stage==9)openEntry("camp_summoning_cradle");
        if(stage==10)openRecipes("tribalpower:binding_effigy");
    }
}
