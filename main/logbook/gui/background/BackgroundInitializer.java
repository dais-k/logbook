package logbook.gui.background;

import java.io.IOException;
import java.util.List;

import logbook.config.AppConfig;
import logbook.config.ShipGroupConfig;
import logbook.constants.AppConstants;
import logbook.data.context.GlobalContext;
import logbook.dto.CreateItemDto;
import logbook.dto.GetShipDto;
import logbook.dto.MissionResultDto;
import logbook.dto.ShipInfoDto;
import logbook.gui.ApplicationMain;
import logbook.gui.logic.CreateReportLogic;
import logbook.internal.BattleResultServer;
import logbook.internal.EnemyData;
import logbook.internal.LoggerHolder;
import logbook.internal.MapEdges;
import logbook.internal.MasterData;
import logbook.internal.ShipParameterRecord;
import logbook.server.proxy.ProxyServer;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * 時間のかかる初期化を別スレッドで実行します
 */
public final class BackgroundInitializer extends Thread {

    private static final LoggerHolder LOG = new LoggerHolder(BackgroundInitializer.class);

    private final Display display;

    /**
     * コンストラクター
     * 
     * @param shell
     */
    public BackgroundInitializer(Shell shell) {
        this.display = shell.getDisplay();
        this.setName("logbook_async_initializer");
    }

    @Override
    public void run() {
        ApplicationMain.sysPrint("バックグラウンド初期化開始");
        try {
            // プロキシサーバーを開始する
            ProxyServer.start();

        } catch (Exception e) {
            LOG.get().warn("サーバ起動に失敗しました", e);
        }
        ApplicationMain.sysPrint("サーバ起動完了");

        ApplicationMain.getUserlogger().get().info(AppConstants.TITLEBAR_TEXT + " 起動しました");
        ApplicationMain.sysPrint("ロガー初期化完了");

        // 設定ファイルを読み込む（遅延初期化が実装されているが先読みしておく）
        try {
            boolean success = true;
            success &= MasterData.INIT_COMPLETE; // MasterData
            ShipGroupConfig.get(); // ShipGroupConfig
            success &= GlobalContext.INIT_COMPLETE; // ItemConfig
            success &= EnemyData.INIT_COMPLETE; // EnemyData
            success &= ShipParameterRecord.INIT_COMPLETE; // ShipParameterRecord

            if (success) {
                this.display.asyncExec(() -> {
                    try {
                        for (ShipInfoDto ship : MasterData.getMaster().getShips().values()) {
                            ShipParameterRecord.update(ship, null);
                        }
                    } catch (Exception e) {
                        LOG.get().warn("データ更新でエラー", e);
                    }
                });
            }
            else {
                LOG.get().warn("設定ファイルの読み込みに失敗したっぽい？");
            }
        } catch (Exception e) {
            LOG.get().warn("設定ファイル読み込みでエラーが発生しました", e);
        }
        ApplicationMain.sysPrint("設定ファイル読み込み完了");

        try {
            // 遠征ログ
            final List<MissionResultDto> missionResultList = AppConfig.get().isLoadMissionLog()
                    ? CreateReportLogic.loadMissionReport()
                    : null;
            if (missionResultList != null) {
                this.display.asyncExec(() -> {
                    try {
                        GlobalContext.addMissionResultList(missionResultList);
                        ApplicationMain.logPrint("遠征ログ読み込み完了(" + missionResultList.size() + "件)");
                    } catch (Exception e) {
                        LOG.get().warn("GUI更新でエラー", e);
                    }
                });
            }
        } catch (Exception e) {
            LOG.get().warn("遠征ログ読み込みでエラー", e);
        }

        try {
            // 建造ログ
            final List<GetShipDto> createShipList = AppConfig.get().isLoadCreateShipLog()
                    ? CreateReportLogic.loadCreateShipReport()
                    : null;
            if (createShipList != null) {
                this.display.asyncExec(() -> {
                    try {
                        GlobalContext.addGetshipList(createShipList);
                        ApplicationMain.logPrint("建造ログ読み込み完了(" + createShipList.size() + "件)");
                    } catch (Exception e) {
                        LOG.get().warn("GUI更新でエラー", e);
                    }
                });
            }
        } catch (Exception e) {
            LOG.get().warn("建造ログ読み込みでエラー", e);
        }

        try {
            // 開発ログ
            final List<CreateItemDto> createItemList = AppConfig.get().isLoadCreateItemLog()
                    ? CreateReportLogic.loadCreateItemReport()
                    : null;
            if (createItemList != null) {
                this.display.asyncExec(() -> {
                    try {
                        GlobalContext.addCreateItemList(createItemList);
                        ApplicationMain.logPrint("開発ログ読み込み完了(" + createItemList.size() + "件)");
                    } catch (Exception e) {
                        LOG.get().warn("GUI更新でエラー", e);
                    }
                });
            }
        } catch (Exception e) {
            LOG.get().warn("開発ログ読み込みでエラー", e);
        }

        try {
            // マップセル読み込み
            MapEdges.load();
            ApplicationMain.logPrint("マップセル読み込み完了");
        } catch (IOException e) {
            LOG.get().warn("マップセルの読み込みに失敗しました", e);
        }

        try {
            // 出撃ログファイル読み込み
            BattleResultServer.load();
            final int failCount = BattleResultServer.get().getFailCount();
            this.display.asyncExec(() -> {
                try {
                    final int numLogRecord = BattleResultServer.get().size();
                    ApplicationMain.logPrint("出撃ログ読み込み完了(" + numLogRecord + "件)");
                    if (failCount > 0) {
                        ApplicationMain.logPrint("注意:" + failCount + "件の出撃ログ読み込みに失敗しています");
                    }
                } catch (Exception e) {
                    LOG.get().warn("GUI更新でエラー", e);
                }
            });
        } catch (Exception e) {
            LOG.get().warn("出撃ログの読み込みに失敗しました (" + AppConfig.get().getBattleLogPath() + ")", e);
        }

        ApplicationMain.logPrint("バックグラウンド初期化完了");
    }
}
