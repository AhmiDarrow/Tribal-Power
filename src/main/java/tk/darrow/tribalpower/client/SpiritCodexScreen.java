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
    private double diagramTime;
    private final long diagramEpoch = System.nanoTime();
    private int left, top, bookWidth, bookHeight, sidebar, contentX, contentWidth, listPage, scroll, maxScroll, recipePage;
    private EditBox search;
    private List<RecipeHolder<?>> recipes=List.of();
    private record Hit(int x,int y,ItemStack item) {}
    private record Link(int y,String id) {}
    private final List<Link> links=new ArrayList<>();

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
            List<String> visible=categories.stream().filter(c->spoilers||c.equals("Contents")||c.equals("Bookmarks")||CodexEntries.ALL.stream().anyMatch(e->e.category().equals(c)&&visible(e))).toList();
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
        button(motion?"Motion: on":"Motion: off",left+bookWidth-98,top+bookHeight-25,88,b->{
            if(motion)diagramTime=(System.nanoTime()-diagramEpoch)/1_000_000_000.0;
            motion=!motion;rebuildWidgets();
        });
        button(recipeItem.isEmpty()?(bookmarks.contains(selected)?"Unmark":"Bookmark"):(showUses?"Show recipes":"Show uses"),contentX,top+60,76,b->{
            if(recipeItem.isEmpty()){if(!bookmarks.remove(selected))bookmarks.add(selected);saveBookmarks();}
            else {showUses=!showUses;recipePage=0;scroll=0;findRecipes();}
            rebuildWidgets();
        });
        if(recipeItem.isEmpty()){
            button("Recipes",contentX+80,top+60,60,b->{Entry e=current(); if(e!=null)openRecipes("tribalpower:"+e.icon());});
            String next=tk.darrow.tribalpower.guide.CodexTutorial.next(selected);
            if(next!=null)button("Next",contentX+144,top+60,48,b->openEntry(next));
            if(!motion) {
                button("< Step",contentX+198,top+60,54,b->diagramTime=Math.max(0,diagramTime-0.5));
                button("Step >",contentX+256,top+60,54,b->diagramTime+=0.5);
            }
        }
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
            for(Entry e:CodexEntries.ALL) if(visible(e)
                    && (category.equals("Contents")||category.equals(e.category())||(category.equals("Bookmarks")&&bookmarks.contains(e.id())))
                    &&(e.title()+" "+e.text()).toLowerCase(Locale.ROOT).contains(needle)) rows.add(new String[]{e.id(),e.title()});
        }
        if(category.equals("Contents"))rows.sort(Comparator.comparingInt(row->{
            int index=tk.darrow.tribalpower.guide.CodexTutorial.SEQUENCE.indexOf(row[0]);
            return index<0?Integer.MAX_VALUE:index;
        }));
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
    /** Spoiler pages stay veiled unless revealed for the session or unlocked in-world (Tribe Mark held, tablet read). */
    private boolean visible(Entry e){return spoilers||!e.spoiler()||CodexUnlocks.unlocked(e);}
    private Entry current(){return CodexEntries.ALL.stream().filter(e->e.id().equals(selected)).findFirst().orElse(CodexEntries.ALL.getFirst());}
    private void openEntry(String id) {
        Entry target=CodexEntries.ALL.stream().filter(e->e.id().equals(id)).findFirst().orElse(null);
        if(target==null)return;
        if(!visible(target)){confirmSpoilers(()->openEntry(id));return;}
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
        for(String part:text.split("\n",-1)) {
            if(part.isBlank()){y+=6;continue;}
            boolean heading=Set.of("Start here","Using this book","Diagrams","When something stops","You need","Steps","Check","Next","If it does not work","If it stays dark","If work stops","Automation","Range","Supply and demand","Important","Timing details","Other inputs","Later","The states","Planning a camp").contains(part);
            for(FormattedCharSequence line:font.split(Component.literal(part),contentWidth-12)){
                g.drawString(font,line,contentX+6,y,heading?GOLD:color,false);y+=12;
            }
        }
        return y+8;
    }
    private void item(GuiGraphics g,ItemStack item,int x,int y) {
        g.fill(x-1,y-1,x+17,y+17,0xFF23373F);g.renderItem(item,x,y);g.renderItemDecorations(font,item,x,y);
        if(y>=top+86&&y+16<=top+bookHeight-55)hits.add(new Hit(x,y,item));
    }

    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        renderBackground(g,mx,my,partial);
        var atlas=ResourceLocation.parse("tribalpower:textures/gui/codex/quest_atlas.png");
        g.blit(atlas,left,top,bookWidth,bookHeight,0F,0F,1536,1024,1536,1024);
        g.fill(left+8,top+8,left+sidebar,top+bookHeight-8,0xCC101C27);
        g.fill(contentX-6,top+80,contentX+contentWidth+6,top+bookHeight-50,0xCC101C27);
        g.drawString(font,"SPIRIT CODEX",left+26,top+12,GOLD,false);
        String mode=spoilers?"THE VEIL IS OPEN":"SPOILER-SAFE";
        g.drawString(font,mode,left+bookWidth-font.width(mode)-26,top+12,TEAL,false);
        if(listButtons.isEmpty())g.drawString(font,"No matching pages",left+12,top+87,0xFF9CB8B9,false);
        hits.clear();int start=top+88;
        g.enableScissor(contentX,start,contentX+contentWidth,top+bookHeight-55);
        int y=start-scroll;
        if(recipeItem.isEmpty()) {
            Entry e=current();links.clear();
            y=paragraph(g,e.title(),y,GOLD);
            y=illustration(g,e,y);
            y=paragraph(g,e.text(),y,PAPER);
            y=paragraph(g,"RELATED TEACHINGS",y+6,TEAL);
            String hint=CodexUnlocks.hint(e.category());
            if(hint!=null)y=paragraph(g,hint,y,TEAL);
            for(Entry other:CodexEntries.ALL) if(!other.id().equals(e.id())&&other.category().equals(e.category())&&visible(other)) {
                g.drawString(font,"> "+font.plainSubstrByWidth(other.title(),contentWidth-22),contentX+6,y,GOLD,false);
                links.add(new Link(y,other.id()));y+=16;
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
                    y=paragraph(g,"Base recipe: "+lattice.seconds()+" seconds | "+lattice.pulse()+" Pulse/s | "+(lattice.seconds()*lattice.pulse())+" total Pulse",y,PAPER);
                    y=paragraph(g,"Power settings, machine rank and local bonuses can change these values. Use the Codex on the placed machine to check its requirements.",y,TEAL);
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

    /**
     * Picture (when the entry has one and it is spoiler-guarded or marked safe) beside or above the animated diagram.
     * Every teaching gets a diagram; wide pages put the two side by side, narrow ones stack them.
     */
    private int illustration(GuiGraphics g,Entry e,int y) {
        int size=Math.min(bookHeight<300?64:112,contentWidth-12),height=CodexDiagrams.height(e);
        boolean cover=e.id().equals("chapter_1");
        boolean picture=cover||(!e.picture().isEmpty()&&(e.spoiler()||e.safePicture()));
        double t=motion?(System.nanoTime()-diagramEpoch)/1_000_000_000.0:diagramTime;
        int diagramX=contentX+4,diagramWidth=contentWidth-8,diagramY=y,bottom=y;
        if(picture) {
            var pic=ResourceLocation.parse("tribalpower:textures/gui/codex/"+(cover?"cover":e.picture())+".png");
            boolean beside=!cover&&contentWidth-size-20>=200;
            int px=beside?contentX+6:contentX+(contentWidth-size)/2;
            g.blit(pic,px,y,0,0,size,size,size,size);
            bottom=y+size;
            if(beside){diagramX=px+size+8;diagramWidth=contentX+contentWidth-4-diagramX;diagramY=y+Math.max(0,(size-height)/2);}
            else diagramY=y+size+8;
        }
        if(cover)return bottom+8;
        CodexDiagrams.draw(g,font,e,diagramX,diagramY,diagramWidth,t,(stack,pos)->item(g,stack,pos[0],pos[1]));
        bottom=Math.max(bottom,diagramY+height);
        return paragraph(g,"Use Motion to pause, then the Step buttons to inspect the diagram. Left / Right also steps when search is not focused. Click an item for Recipes. Placement views: top is north, one square is one block.",bottom+6,0xFF9CB8B9);
    }

    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(x>=contentX){scroll=Math.clamp(scroll-(int)(vertical*24),0,maxScroll);return true;}
        listPage=Math.max(0,listPage+(vertical<0?1:-1));rebuildWidgets();return true;
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0&&y>=top+88&&y<top+bookHeight-55) {
            for(Hit h:hits)if(x>=h.x&&x<h.x+16&&y>=h.y&&y<h.y+16){openRecipes(BuiltInRegistries.ITEM.getKey(h.item.getItem()).toString());return true;}
            if(recipeItem.isEmpty()&&x>=contentX&&x<contentX+contentWidth)
                for(Link link:links)if(y>=link.y()&&y<link.y()+16&&link.y()>=top+88&&link.y()+16<=top+bookHeight-55){openEntry(link.id());return true;}
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(!search.isFocused()&&(key==266||key==267)){scroll=Math.clamp(scroll+(key==266?-100:100),0,maxScroll);return true;}
        if(!search.isFocused()&&!recipeItem.isEmpty()&&(key==262||key==263)){recipePage+=key==262?1:-1;return true;}
        if(!search.isFocused()&&recipeItem.isEmpty()&&!motion&&(key==262||key==263)){diagramTime=Math.max(0,diagramTime+(key==262?0.5:-0.5));return true;}
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
