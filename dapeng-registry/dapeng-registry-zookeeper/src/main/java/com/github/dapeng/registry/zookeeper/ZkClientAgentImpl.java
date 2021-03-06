package com.github.dapeng.registry.zookeeper;


import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.router.Route;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lihuimin
 * @date 2017/12/24
 */
public class ZkClientAgentImpl implements ZkClientAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkClientAgentImpl.class);
    /**
     * 是否使用 灰度 zk
     */
    private final boolean usingFallbackZk = SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_ISCONFIG;

    private ClientZk masterZk, fallbackZk;

    /**
     * 路由配置信息
     */
    private final Map<String, List<Route>> routesMap = new ConcurrentHashMap<>(16);


    public ZkClientAgentImpl() {
        start();
    }

    @Override
    public void start() {

        masterZk = new ClientZk(SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST);
        masterZk.init();

        if (SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_ISCONFIG) {
            fallbackZk = new ClientZk(SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_HOST);
            fallbackZk.init();
        }
    }

    //todo 优雅退出的时候, 需要调用这个
    @Override
    public void stop() {
        if (masterZk != null) {
            masterZk.destroy();
        }

        if (fallbackZk != null) {
            fallbackZk.destroy();
        }

    }

    @Override
    public void cancelSyncService(ZkServiceInfo zkInfo) {
        //fixme should remove the debug log
        LOGGER.info("cancelSyncService:[" + zkInfo.service + "]");
        zkInfo.setStatus(ZkServiceInfo.Status.CANCELED);
    }

    @Override
    public void syncService(ZkServiceInfo zkInfo) {
        if (zkInfo.getStatus() != ZkServiceInfo.Status.ACTIVE) {
            LOGGER.info(getClass().getSimpleName() + "::syncService[serviceName:" + zkInfo.service + "]:zkInfo just created, now sync with zk");
            masterZk.syncServiceZkInfo(zkInfo);
            if (zkInfo.getStatus() != ZkServiceInfo.Status.ACTIVE && usingFallbackZk) {
                fallbackZk.syncServiceZkInfo(zkInfo);
            }

            LOGGER.info(getClass().getSimpleName() + "::syncService[serviceName:" + zkInfo.service + ", status:" + zkInfo.getStatus() + "]");
        }

        //使用路由规则，过滤可用服务器
        // fixme 在runtime跟config变化的时候才需要计算可用节点信息
        InvocationContext context = InvocationContextImpl.Factory.currentInstance();
//        List<Route> routes = usingFallbackZk ? fallbackZk.getRoutes() : masterZk.getRoutes();
//        List<RuntimeInstance> runtimeList = new ArrayList<>();

        if (zkInfo.getStatus() == ZkServiceInfo.Status.ACTIVE && zkInfo.getRuntimeInstances() != null) {

            LOGGER.info(getClass().getSimpleName() + "::syncService[serviceName:" + zkInfo.service + "]:zkInfo succeed");
        } else {
            LOGGER.info(getClass().getSimpleName() + "::syncService[serviceName:" + zkInfo.service + "]:zkInfo failed");
        }
    }

    @Override
    public List<Route> getRoutes(String service) {
        return masterZk.getRoutes(service);
    }
}
