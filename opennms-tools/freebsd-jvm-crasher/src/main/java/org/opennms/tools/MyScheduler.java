package org.opennms.tools;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.UnableToInterruptJobException;
import org.quartz.spi.JobFactory;

public class MyScheduler implements Scheduler {

    @Override
    public void addCalendar(String arg0, Calendar arg1, boolean arg2,
            boolean arg3) throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addGlobalJobListener(JobListener arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addGlobalTriggerListener(TriggerListener arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addJob(JobDetail arg0, boolean arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addJobListener(JobListener arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addSchedulerListener(SchedulerListener arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addTriggerListener(TriggerListener arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean deleteCalendar(String arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteJob(String arg0, String arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Calendar getCalendar(String arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getCalendarNames() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SchedulerContext getContext() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List getCurrentlyExecutingJobs() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JobListener getGlobalJobListener(String arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List getGlobalJobListeners() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TriggerListener getGlobalTriggerListener(String arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List getGlobalTriggerListeners() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JobDetail getJobDetail(String arg0, String arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getJobGroupNames() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JobListener getJobListener(String arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set getJobListenerNames() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getJobNames(String arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SchedulerMetaData getMetaData() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set getPausedTriggerGroups() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSchedulerInstanceId() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List getSchedulerListeners() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSchedulerName() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Trigger getTrigger(String arg0, String arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getTriggerGroupNames() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TriggerListener getTriggerListener(String arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set getTriggerListenerNames() throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getTriggerNames(String arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getTriggerState(String arg0, String arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Trigger[] getTriggersOfJob(String arg0, String arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean interrupt(String arg0, String arg1)
            throws UnableToInterruptJobException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isInStandbyMode() throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isPaused() throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isShutdown() throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isStarted() throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void pause() throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void pauseAll() throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void pauseJob(String arg0, String arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void pauseJobGroup(String arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void pauseTrigger(String arg0, String arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void pauseTriggerGroup(String arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean removeGlobalJobListener(JobListener arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeGlobalJobListener(String arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeGlobalTriggerListener(TriggerListener arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeGlobalTriggerListener(String arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeJobListener(String arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeSchedulerListener(SchedulerListener arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeTriggerListener(String arg0)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Date rescheduleJob(String arg0, String arg1, Trigger arg2)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void resumeAll() throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resumeJob(String arg0, String arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resumeJobGroup(String arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resumeTrigger(String arg0, String arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resumeTriggerGroup(String arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Date scheduleJob(Trigger arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date scheduleJob(JobDetail arg0, Trigger arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setJobFactory(JobFactory arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void shutdown() throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void shutdown(boolean arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void standby() throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void start() throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void startDelayed(int arg0) throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void triggerJob(String arg0, String arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void triggerJob(String arg0, String arg1, JobDataMap arg2)
            throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void triggerJobWithVolatileTrigger(String arg0, String arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void triggerJobWithVolatileTrigger(String arg0, String arg1,
            JobDataMap arg2) throws SchedulerException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean unscheduleJob(String arg0, String arg1)
            throws SchedulerException {
        // TODO Auto-generated method stub
        return false;
    }
}