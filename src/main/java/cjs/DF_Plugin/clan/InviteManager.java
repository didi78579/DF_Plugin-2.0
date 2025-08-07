package cjs.DF_Plugin.clan;

import cjs.DF_Plugin.DF_Main;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 클랜 초대 시스템을 관리하는 클래스
 */
public class InviteManager {

    // Cache<초대받은 플레이어 UUID, 클랜 이름>
    private final Cache<UUID, String> invites;

    public InviteManager(DF_Main plugin) {
        this.invites = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES) // 5분 후 초대 만료
                .build();
        plugin.getLogger().info("InviteManager loaded.");
    }

    public void invitePlayer(UUID invited, String clanName) {
        invites.put(invited, clanName);
    }

    public String getInvite(UUID invitedUuid) { return invites.getIfPresent(invitedUuid); }

    public void removeInvite(UUID invitedUuid) { invites.invalidate(invitedUuid); }
}