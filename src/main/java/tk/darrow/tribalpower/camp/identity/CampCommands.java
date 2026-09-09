package tk.darrow.tribalpower.camp.identity;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import tk.darrow.tribalpower.camp.CampHooks;

/** {@code /tribalpower camp create|invite|join|leave|kick|info|rename}. Shared with the Camp Charter item. */
public final class CampCommands {
    private CampCommands(){}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tribalpower").then(Commands.literal("camp")
            .then(Commands.literal("create").then(Commands.argument("name",StringArgumentType.greedyString()).executes(c->create(c,StringArgumentType.getString(c,"name")))))
            .then(Commands.literal("invite").then(Commands.argument("player",EntityArgument.player()).executes(c->invite(c,EntityArgument.getPlayer(c,"player")))))
            .then(Commands.literal("join").then(Commands.argument("name",StringArgumentType.greedyString()).executes(c->join(c,StringArgumentType.getString(c,"name")))))
            .then(Commands.literal("leave").executes(CampCommands::leave))
            .then(Commands.literal("kick").then(Commands.argument("player",EntityArgument.player()).executes(c->kick(c,EntityArgument.getPlayer(c,"player")))))
            .then(Commands.literal("info").executes(CampCommands::info))
            .then(Commands.literal("rename").then(Commands.argument("name",StringArgumentType.greedyString()).executes(c->rename(c,StringArgumentType.getString(c,"name")))))));
    }
    private static Component msg(String key,Object... args) { return Component.translatable("message.tribalpower.camp."+key,args); }
    private static int fail(CommandContext<CommandSourceStack> c,String key,Object... args) { c.getSource().sendFailure(msg(key,args));return 0; }
    private static int ok(CommandContext<CommandSourceStack> c,String key,Object... args) { c.getSource().sendSuccess(()->msg(key,args),false);return 1; }

    public static int create(CommandContext<CommandSourceStack> c,String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=c.getSource().getPlayerOrException();
        var data=Camps.data(player.server);
        if(data.campOf(player.getUUID())!=null)return fail(c,"already_member");
        var camp=data.create(name,player.getUUID());
        if(camp==null)return fail(c,"bad_name");
        CampHooks.award(player.serverLevel(),player.getUUID(),"first_camp");
        return ok(c,"created",camp.name);
    }
    public static int invite(CommandContext<CommandSourceStack> c,ServerPlayer invitee) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=c.getSource().getPlayerOrException();
        return switch(doInvite(player,invitee)) { case 0 -> fail(c,"no_camp"); case 1 -> fail(c,"target_has_camp",invitee.getDisplayName()); default -> ok(c,"invited",invitee.getDisplayName()); };
    }
    /** 0: inviter has no camp, 1: invitee already in a camp, 2: invitation sent. */
    public static int doInvite(ServerPlayer inviter,Player invitee) {
        var data=Camps.data(inviter.server);
        var camp=data.campOf(inviter.getUUID());
        if(camp==null)return 0;
        if(!data.invite(camp,inviter.getUUID(),invitee.getUUID()))return 1;
        invitee.displayClientMessage(msg("invitation",inviter.getDisplayName(),camp.name,camp.name),false);
        return 2;
    }
    public static void inviteFromCharter(ServerPlayer holder,Player invitee) {
        switch(doInvite(holder,invitee)) {
            case 0 -> holder.displayClientMessage(msg("charter_no_camp"),false);
            case 1 -> holder.displayClientMessage(msg("target_has_camp",invitee.getDisplayName()),true);
            default -> holder.displayClientMessage(msg("invited",invitee.getDisplayName()),true);
        }
    }
    public static int join(CommandContext<CommandSourceStack> c,String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=c.getSource().getPlayerOrException();
        var data=Camps.data(player.server);
        var camp=data.byName(name);
        if(camp==null)return fail(c,"unknown",name);
        if(data.campOf(player.getUUID())!=null)return fail(c,"already_member");
        if(!data.join(camp,player.getUUID()))return fail(c,"not_invited",camp.name);
        broadcast(player.server,camp,msg("joined_broadcast",player.getDisplayName(),camp.name));
        return 1;
    }
    public static int leave(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=c.getSource().getPlayerOrException();
        var data=Camps.data(player.server);
        var camp=data.campOf(player.getUUID());
        if(camp==null)return fail(c,"no_camp");
        data.leave(player.getUUID());
        if(camp.members.isEmpty()) { Containers.dropContents(player.level(),player.blockPosition(),new net.minecraft.world.SimpleContainer(camp.vault.toArray(new net.minecraft.world.item.ItemStack[0])));camp.vault.clear();return ok(c,"dissolved",camp.name); }
        broadcast(player.server,camp,msg("left_broadcast",player.getDisplayName(),camp.name));
        return ok(c,"left",camp.name);
    }
    public static int kick(CommandContext<CommandSourceStack> c,ServerPlayer target) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=c.getSource().getPlayerOrException();
        var data=Camps.data(player.server);
        var camp=data.campOf(player.getUUID());
        if(camp==null)return fail(c,"no_camp");
        if(!camp.leader.equals(player.getUUID()))return fail(c,"not_leader");
        if(target.getUUID().equals(player.getUUID()))return fail(c,"kick_self");
        if(!camp.isMember(target.getUUID()))return fail(c,"not_member",target.getDisplayName());
        data.leave(target.getUUID());
        target.displayClientMessage(msg("kicked",camp.name),false);
        return ok(c,"kicked_other",target.getDisplayName());
    }
    public static int info(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=c.getSource().getPlayerOrException();
        var data=Camps.data(player.server);
        var camp=data.campOf(player.getUUID());
        if(camp==null) {
            var invite=data.invitation(player.getUUID());
            if(invite!=null && data.camp(invite.camp())!=null)return ok(c,"info_invited",data.camp(invite.camp()).name);
            return fail(c,"no_camp");
        }
        StringBuilder names=new StringBuilder();
        for(UUID member:camp.members) { if(names.length()>0)names.append(", ");names.append(name(player.server,member)); }
        int anchors=CampHooks.campAnchors(player.server,camp.id);
        return ok(c,"info",camp.name,name(player.server,camp.leader),camp.members.size(),names.toString(),anchors,CampHooks.CAMP_ANCHOR_BUDGET,String.format("#%06x",camp.colour));
    }
    public static int rename(CommandContext<CommandSourceStack> c,String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=c.getSource().getPlayerOrException();
        var data=Camps.data(player.server);
        var camp=data.campOf(player.getUUID());
        if(camp==null)return fail(c,"no_camp");
        if(!camp.leader.equals(player.getUUID()))return fail(c,"not_leader");
        if(!data.rename(camp,name))return fail(c,"bad_name");
        return ok(c,"renamed",camp.name);
    }
    private static String name(MinecraftServer server,UUID id) {
        var online=server.getPlayerList().getPlayer(id);
        if(online!=null)return online.getGameProfile().getName();
        return server.getProfileCache()==null?id.toString().substring(0,8):server.getProfileCache().get(id).map(p->p.getName()).orElse(id.toString().substring(0,8));
    }
    private static void broadcast(MinecraftServer server,CampSavedData.Camp camp,Component message) {
        for(UUID member:camp.members) { var online=server.getPlayerList().getPlayer(member);if(online!=null)online.displayClientMessage(message,false); }
    }
}
