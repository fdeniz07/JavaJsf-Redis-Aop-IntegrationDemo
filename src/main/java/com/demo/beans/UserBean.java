package com.demo.beans;

import com.demo.dao.UserDao;
import com.demo.model.User;
import com.demo.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Named
@ViewScoped
public class UserBean implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(UserBean.class);
    private static final long serialVersionUID = 1L;

    private LazyDataModel<User> lazyModel;
    private UserDao userDao;
    private RedisService redisService;
    private boolean dbButtonDisabled;
    private boolean redisButtonEnabled;
    private boolean loadingFromRedis;
    private boolean dataLoaded;
    private int rowsPerPage = 50;

    // Performance metrics
    private long dbLoadTime;
    private long redisLoadTime;
    private long redisSyncTime;
    private long dbDataSize;
    private long redisDataSize;
    private int totalRecords;

    @PostConstruct
    public void init() {
        userDao = new UserDao();
        redisService = new RedisService();
        dbButtonDisabled = false;
        redisButtonEnabled = false;
        loadingFromRedis = false;
        dataLoaded = false;
        resetMetrics();

        userDao.createTable();
        initializeLazyModel();
    }

    private void resetMetrics() {
        dbLoadTime = 0;
        redisLoadTime = 0;
        redisSyncTime = 0;
        dbDataSize = 0;
        redisDataSize = 0;
        totalRecords = 0;
    }

    private void initializeLazyModel() {
        lazyModel = new LazyDataModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<User> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, FilterMeta> filters) {
                try {
                    if (!dataLoaded) {
                        logger.debug("Data not loaded yet, returning empty list");
                        return new ArrayList<>();
                    }

                    logger.debug("Loading data: first={}, pageSize={}, sortField={}",
                            first, pageSize, sortField);

                    if (loadingFromRedis) {
                        int total = redisService.getTotalCountFromRedis();
                        this.setRowCount(total);
                        return redisService.getUsersFromRedis(first, pageSize);
                    } else {
                        int total = userDao.getTotalCount();
                        this.setRowCount(total);
                        return userDao.getAllUsers(first, pageSize);
                    }
                } catch (Exception e) {
                    logger.error("Error loading data", e);
                    return new ArrayList<>();
                }
            }

            @Override
            public User getRowData(String rowKey) {
                if (rowKey == null || !dataLoaded) {
                    return null;
                }

                try {
                    Long id = Long.valueOf(rowKey);
                    if (loadingFromRedis) {
                        return redisService.getUserFromRedis(id);
                    } else {
                        return userDao.getUserById(id);
                    }
                } catch (Exception e) {
                    logger.error("Error getting row data for key: " + rowKey, e);
                    return null;
                }
            }

            @Override
            public String getRowKey(User user) {
                return user != null ? user.getId().toString() : null;
            }
        };

        lazyModel.setRowCount(0);
    }

    public void loadFromDatabase() {
        try {
            logger.info("Loading data from database");
            long startTime = System.currentTimeMillis();

            loadingFromRedis = false;
            dbButtonDisabled = true;
            redisButtonEnabled = true;
            dataLoaded = true;

            // Get all data and calculate size
            List<User> allData = userDao.getAllUsers(0, Integer.MAX_VALUE);
            dbDataSize = calculateDataSize(allData);
            totalRecords = allData.size();

            // Calculate loading time
            dbLoadTime = System.currentTimeMillis() - startTime;

            lazyModel.load(0, rowsPerPage, null, SortOrder.ASCENDING, null);

            logger.info("Database metrics - Load time: {}ms, Data size: {} bytes, Total records: {}",
                    dbLoadTime, dbDataSize, totalRecords);
        } catch (Exception e) {
            logger.error("Error loading from database", e);
            resetMetrics();
        }
    }

    public void loadFromRedis() {
        try {
            logger.info("Loading data from Redis");
            long syncStartTime = System.currentTimeMillis();

            // Sync data to Redis
            List<User> dbUsers = userDao.getAllUsers(0, Integer.MAX_VALUE);
            redisService.syncDatabaseToRedis(dbUsers);
            redisSyncTime = System.currentTimeMillis() - syncStartTime;

            // Start Redis load timing
            long loadStartTime = System.currentTimeMillis();
            loadingFromRedis = true;
            redisButtonEnabled = false;
            dataLoaded = true;

            // Get all data and calculate size
            List<User> allRedisData = redisService.getUsersFromRedis(0, Integer.MAX_VALUE);
            redisDataSize = calculateDataSize(allRedisData);
            totalRecords = allRedisData.size();

            // Calculate loading time
            redisLoadTime = System.currentTimeMillis() - loadStartTime;

            lazyModel.load(0, rowsPerPage, null, SortOrder.ASCENDING, null);

            logger.info("Redis metrics - Load time: {}ms, Sync time: {}ms, Data size: {} bytes, Total records: {}",
                    redisLoadTime, redisSyncTime, redisDataSize, totalRecords);
        } catch (Exception e) {
            logger.error("Error loading from Redis", e);
            resetMetrics();
        }
    }

    private long calculateDataSize(List<User> users) {
        if (users == null || users.isEmpty()) {
            return 0;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            long totalSize = 0;
            for (User user : users) {
                String json = mapper.writeValueAsString(user);
                totalSize += json.getBytes().length;
            }
            return totalSize;
        } catch (Exception e) {
            logger.error("Error calculating data size", e);
            return 0;
        }
    }

    public void onRowsPerPageChange() {
        logger.info("Rows per page changed to: {}", rowsPerPage);
        if (dataLoaded) {
            lazyModel.load(0, rowsPerPage, null, SortOrder.ASCENDING, null);
        }
    }

    // Getters and Setters
    public LazyDataModel<User> getLazyModel() {
        return lazyModel;
    }

    public void setLazyModel(LazyDataModel<User> lazyModel) {
        this.lazyModel = lazyModel;
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(int rowsPerPage) {
        this.rowsPerPage = rowsPerPage;
    }

    public boolean isDbButtonDisabled() {
        return dbButtonDisabled;
    }

    public void setDbButtonDisabled(boolean dbButtonDisabled) {
        this.dbButtonDisabled = dbButtonDisabled;
    }

    public boolean isRedisButtonEnabled() {
        return redisButtonEnabled;
    }

    public void setRedisButtonEnabled(boolean redisButtonEnabled) {
        this.redisButtonEnabled = redisButtonEnabled;
    }

    public String getDbLoadTime() {
        return String.format("%.2f sec", dbLoadTime / 1000.0);
    }

    public String getRedisLoadTime() {
        return String.format("%.2f sec", redisLoadTime / 1000.0);
    }

    public String getRedisSyncTime() {
        return String.format("%.2f sec", redisSyncTime / 1000.0);
    }

    public String getDbDataSize() {
        return formatDataSize(dbDataSize);
    }

    public String getRedisDataSize() {
        return formatDataSize(redisDataSize);
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    private String formatDataSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}