package com.sap.jenkinsci.plugin.remote_view;

/**
 * Created by @NutellaMitBrezel on 09.06.2015.
 */

import hudson.Extension;
import hudson.security.Permission;
import hudson.util.FormValidation;

import java.io.IOException;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;


public class RemoteJobsSection extends SectionedViewSection{


    @DataBoundConstructor
    public RemoteJobsSection(String name, String UserName, String Password, Width width, Positioning alignment, String remoteURL) {
        super(name, UserName, Password, width, alignment, remoteURL);
    }

    public static int var = 0;

    @Extension
    public static final class DescriptorImpl extends SectionedViewSectionDescriptor {

        @Override
        public SectionedViewSection newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            RemoteJobsSection section = (RemoteJobsSection) super.newInstance(req, formData);

            return section;
        }

        @Override
        public String getDisplayName() {
            return "Remote Jobs Section";
        }

        @RequirePOST
        public FormValidation doCheckRemoteURL(@QueryParameter String value) throws IOException, ServletException {
            if (!Jenkins.get().hasPermission(Permission.CONFIGURE)) {
                return FormValidation.error("You don't have permission to perform this operation.");
            }

            if (value.isEmpty()) {
                return FormValidation.error("Do not forget to specify the URL of your remote Jenkins!");
            }
            return FormValidation.ok();
        }
    }

}

