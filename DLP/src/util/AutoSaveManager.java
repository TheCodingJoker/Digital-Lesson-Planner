package util;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoSaveManager {
    
    private static AutoSaveManager instance;
    private ScheduledExecutorService scheduler;
    private Map<String, Object> formCache;
    private String cacheDirectory;
    private ErrorLogger errorLogger;
    
    private AutoSaveManager() {
        this.formCache = new HashMap<>();
        this.errorLogger = ErrorLogger.getInstance();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Initialize cache directory
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            appData = System.getProperty("user.home");
        }
        cacheDirectory = appData + File.separator + "DLP" + File.separator + "Cache";
        
        // Create cache directory if it doesn't exist
        File cacheDir = new File(cacheDirectory);
        if (!cacheDir.exists()) {
            if (cacheDir.mkdirs()) {
                System.out.println("Created cache directory: " + cacheDirectory);
            }
        }
        
        // Start auto-save scheduler (every 30 seconds)
        startAutoSaveScheduler();
    }
    
    public static synchronized AutoSaveManager getInstance() {
        if (instance == null) {
            instance = new AutoSaveManager();
        }
        return instance;
    }
    
    private void startAutoSaveScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                saveCacheToDisk();
            } catch (Exception e) {
                errorLogger.logError("AutoSaveManager", "autoSave", "Auto-save failed", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    public void cacheFormData(String formId, Object formData) {
        formCache.put(formId, formData);
        errorLogger.logInfo("AutoSaveManager", "cacheFormData", "Cached form data for: " + formId);
    }
    
    public Object getCachedFormData(String formId) {
        return formCache.get(formId);
    }
    
    public boolean hasCachedData(String formId) {
        return formCache.containsKey(formId);
    }
    
    public void clearCache(String formId) {
        formCache.remove(formId);
        // Also remove from disk
        File cacheFile = new File(cacheDirectory + File.separator + formId + ".cache");
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
        errorLogger.logInfo("AutoSaveManager", "clearCache", "Cleared cache for: " + formId);
    }
    
    public void clearAllCache() {
        formCache.clear();
        // Clear all cache files
        File cacheDir = new File(cacheDirectory);
        File[] cacheFiles = cacheDir.listFiles((dir, name) -> name.endsWith(".cache"));
        if (cacheFiles != null) {
            for (File file : cacheFiles) {
                file.delete();
            }
        }
        errorLogger.logInfo("AutoSaveManager", "clearAllCache", "Cleared all cache");
    }
    
    private void saveCacheToDisk() {
        if (formCache.isEmpty()) {
            return;
        }
        
        try {
            for (Map.Entry<String, Object> entry : formCache.entrySet()) {
                String formId = entry.getKey();
                Object data = entry.getValue();
                
                String cacheFilePath = cacheDirectory + File.separator + formId + ".cache";
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cacheFilePath))) {
                    oos.writeObject(data);
                }
            }
            errorLogger.logInfo("AutoSaveManager", "saveCacheToDisk", "Auto-saved " + formCache.size() + " forms to disk");
        } catch (IOException e) {
            errorLogger.logError("AutoSaveManager", "saveCacheToDisk", "Failed to save cache to disk", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> restoreCacheFromDisk() {
        Map<String, Object> restoredCache = new HashMap<>();
        File cacheDir = new File(cacheDirectory);
        
        if (!cacheDir.exists()) {
            return restoredCache;
        }
        
        File[] cacheFiles = cacheDir.listFiles((dir, name) -> name.endsWith(".cache"));
        if (cacheFiles == null) {
            return restoredCache;
        }
        
        for (File cacheFile : cacheFiles) {
            String formId = cacheFile.getName().replace(".cache", "");
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile))) {
                Object data = ois.readObject();
                restoredCache.put(formId, data);
                formCache.put(formId, data); // Also restore to memory cache
                errorLogger.logInfo("AutoSaveManager", "restoreCacheFromDisk", "Restored cache for: " + formId);
            } catch (IOException | ClassNotFoundException e) {
                errorLogger.logError("AutoSaveManager", "restoreCacheFromDisk", 
                    "Failed to restore cache for: " + formId, e);
                // Delete corrupted cache file
                cacheFile.delete();
            }
        }
        
        return restoredCache;
    }
    
    public void shutdown() {
        // Save cache one more time before shutdown
        saveCacheToDisk();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public String getCacheDirectory() {
        return cacheDirectory;
    }
}