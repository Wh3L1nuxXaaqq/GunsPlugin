package invoke.plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class RecoilManager {

    static ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    public static void applyRecoil(Player player, Gun gun) {
        final float yawRecoil = -Math.abs((float) Math.random() * gun.recoilYawDef * 6.0f);
        final float pitchRecoil = -Math.abs((float) Math.random() * gun.recoilPitchDef * 6.0f);

        float yawMultiplier = 1.0f;
        float pitchMultiplier = 1.0f;

        boolean isAiming = false;
        PersistentDataContainer pdcPlayer = player.getPersistentDataContainer();
        if (pdcPlayer.has(Gun.aimingKey, PersistentDataType.BYTE)) {
            isAiming = pdcPlayer.get(Gun.aimingKey, PersistentDataType.BYTE) == (byte)1;
        }

        if (isAiming) {
            yawMultiplier = normalizeShift(gun.recoilYawShift);
            pitchMultiplier = normalizeShift(gun.recoilPitchShift);
        }
        if (player.getWalkSpeed() > 0.1) {
            yawMultiplier *= 1.3f;
            pitchMultiplier *= 1.3f;
        }

        final float finalYawRecoil = yawRecoil * yawMultiplier;
        final float finalPitchRecoil = pitchRecoil * pitchMultiplier;
        Location loc = player.getLocation();
        float newYaw = loc.getYaw() + finalYawRecoil;
        float newPitch = Math.max(Math.min(loc.getPitch() + finalPitchRecoil, 90), -90);
        sendRotationPacket(player, newYaw, newPitch);
    }

    private static float normalizeShift(float shift) {
        if (shift > 1.0f) {
            return shift / 100.0f;
        }
        return shift;
    }

    private static void sendRotationPacket(Player player, float yaw, float pitch) {
        PacketContainer headRotationPacket = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        headRotationPacket.getIntegers().write(0, player.getEntityId());
        headRotationPacket.getBytes().write(0, (byte) (yaw * 256.0F / 360.0F));
        PacketContainer entityLookPacket = new PacketContainer(PacketType.Play.Server.ENTITY_LOOK);
        entityLookPacket.getIntegers().write(0, player.getEntityId());
        entityLookPacket.getBytes().write(0, (byte) (yaw * 256.0F / 360.0F));
        entityLookPacket.getBytes().write(1, (byte) (pitch * 256.0F / 360.0F));
        entityLookPacket.getBooleans().write(0, player.isOnGround());

        try {
            protocolManager.sendServerPacket(player, headRotationPacket);
            protocolManager.sendServerPacket(player, entityLookPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
