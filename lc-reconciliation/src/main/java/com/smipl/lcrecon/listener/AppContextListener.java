package com.smipl.lcrecon.listener;

import com.smipl.lcrecon.dao.*;
import com.smipl.lcrecon.job.JobManager;
import com.smipl.lcrecon.util.DbUtil;
import com.smipl.lcrecon.util.FileUtil;
import com.smipl.lcrecon.util.PromptLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AppContextListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("LC Reconciliation System starting up...");
        ServletContext ctx = sce.getServletContext();

        // WEB_VUL_11 (XXE/SSRF): forbid external DTD/schema resolution for any XML processing
        // in the app or its libraries. Combined with the upload allowlist (which rejects .xml),
        // this prevents external-entity driven outbound requests.
        System.setProperty("javax.xml.accessExternalDTD", "");
        System.setProperty("javax.xml.accessExternalSchema", "");

        DbUtil.initialize();
        DbUtil.runScript("db/schema.sql");
        DbUtil.runScript("db/seed.sql");

        String catalinaBase = System.getProperty("catalina.base", System.getProperty("user.dir"));
        FileUtil.initialize(catalinaBase);

        ctx.setAttribute("userDao", new UserDao());
        ctx.setAttribute("settingsDao", new SettingsDao());
        ctx.setAttribute("documentTypeDao", new DocumentTypeDao());
        ctx.setAttribute("promptTemplateDao", new PromptTemplateDao());
        ctx.setAttribute("jobDao", new JobDao());
        ctx.setAttribute("lcDocumentDao", new LcDocumentDao());
        ctx.setAttribute("emailGroupDao", new EmailGroupDao());
        ctx.setAttribute("emailTemplateDao", new EmailTemplateDao());
        ctx.setAttribute("jobCostDao", new JobCostDao());

        JobManager jobManager = new JobManager();
        ctx.setAttribute("jobManager", jobManager);

        // Prompt templates have been loaded. To reload from files, uncomment:
        // PromptLoader.loadPrompts(new PromptTemplateDao(), new DocumentTypeDao());

        logger.info("LC Reconciliation System started successfully");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("LC Reconciliation System shutting down...");
        JobManager jobManager = (JobManager) sce.getServletContext().getAttribute("jobManager");
        if (jobManager != null) {
            jobManager.shutdown();
        }
        DbUtil.shutdown();
        logger.info("LC Reconciliation System shut down complete");
    }
}
