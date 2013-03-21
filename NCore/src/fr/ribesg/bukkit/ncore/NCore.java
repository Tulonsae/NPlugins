package fr.ribesg.bukkit.ncore;

import lombok.Getter;
import lombok.Setter;

import org.bukkit.plugin.java.JavaPlugin;

import fr.ribesg.bukkit.ncore.nodes.chat.TalkNode;
import fr.ribesg.bukkit.ncore.nodes.cuboid.CuboidNode;
import fr.ribesg.bukkit.ncore.nodes.dodgeball.DodgeBallNode;
import fr.ribesg.bukkit.ncore.nodes.enchantingegg.EnchantingEggNode;
import fr.ribesg.bukkit.ncore.nodes.general.GeneralNode;
import fr.ribesg.bukkit.ncore.nodes.player.PlayerNode;
import fr.ribesg.bukkit.ncore.nodes.theendagain.TheEndAgainNode;

/**
 * The Core of the N Plugin Suite
 * 
 * @author Ribesg
 */
public class NCore extends JavaPlugin {
    
    @Getter @Setter private TalkNode          talkNode;
    @Getter @Setter private CuboidNode        cuboidNode;
    @Getter @Setter private DodgeBallNode     dodgeBallNode;
    @Getter @Setter private EnchantingEggNode enchantingEggNode;
    @Getter @Setter private GeneralNode       generalNode;
    @Getter @Setter private PlayerNode        playerNode;
    @Getter @Setter private TheEndAgainNode   theEndAgainNode;
    
    @Override
    public void onEnable() {
        // Nothing yet 
    }
    
    @Override
    public void onDisable() {
        // Nothing yet
    }
}
