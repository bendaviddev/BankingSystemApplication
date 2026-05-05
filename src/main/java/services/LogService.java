package main.java.services;

import main.java.enums.ActivityType;
import main.java.models.Log;
import main.java.repositories.LogRepository;

import java.util.ArrayList;

public class LogService {

    private LogRepository logRepository = new LogRepository();

    public void addLog(int userId, ActivityType activityType, String message) {
        Log log = new Log(0, userId, activityType, message);
        logRepository.save(log);
    }

    public ArrayList<Log> getLogsByUserId(int userId) {
        return logRepository.findByUserId(userId);
    }

    public ArrayList<Log> getAllLogs() {
        return logRepository.findAll();
    }
}