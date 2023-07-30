package com.sap.jenkinsci.plugin.remote_view;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.*;
import hudson.util.EnumConverter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.ServletException;

/**
 * Created by @NutellaMitBrezel on 09.06.2015.
 */
public abstract class SectionedViewSection implements ExtensionPoint, Describable<SectionedViewSection> {

  private final static Logger logger = Logger.getLogger(SectionedViewSection.class.getName());

  private String name = null;
  private String remoteURL = null;

  private String UserName = null;

  private String Password = null;
  private List<RemoteJob> remoteJobs = null;
  private Map<String, Wrapper> displayJobs = new HashMap<String, Wrapper>();

  private boolean blue, red, yellow, aborted, filterEnabled, available = false;

  private int counter = 0;

  public SectionedViewSection(String name, String UserName, String Password, Width width, Positioning alignment, String remoteURL) {
    this.setName(name);
    this.setWidth(width);
    this.setAlignment(alignment);
    this.setRemoteURL(remoteURL);
    this.setUserName(UserName);
    this.setPassword(Password);

    determineCss();
  }

  @JavaScriptMethod
  public int getCounter() {
    return this.counter;
  }

  public void setCounter(int counter) {
    this.counter = counter;
  }

  private Width width;

  private Positioning alignment;

  transient String css;

  /**
   * Returns all the registered {@link SectionedViewSection} descriptors.
   */
  public static DescriptorExtensionList<SectionedViewSection, SectionedViewSectionDescriptor> all() {
    return Jenkins.getInstance().<SectionedViewSection, SectionedViewSectionDescriptor> getDescriptorList(
        SectionedViewSection.class);
  }

  public boolean getAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  public String getUserName() {
    return UserName;
  }
  public void setUserName(String UserName) {
    this.UserName = UserName;
  }

  public String getPassword() {
    return Password;
  }

  public void setPassword(String Password) {
    this.Password = Password;
  }

  public String getRemoteURL() {
    return remoteURL;
  }

  public void setRemoteURL(String remoteURL) {

    if (remoteURL.trim().endsWith("/")) {
      this.remoteURL = remoteURL.trim();
    } else {
      this.remoteURL = remoteURL.trim() + "/";
    }

  }

  public Map<String, Wrapper> getDisplayJobs() {
    return this.displayJobs;
  }

  public void setDisplayJobs(Map<String, Wrapper> displayJobs) {
    this.displayJobs = displayJobs;
  }

  public boolean isBlue() {
    return blue;
  }

  public boolean isRed() {
    return red;
  }

  public boolean isYellow() {
    return yellow;
  }

  public boolean isAborted() {
    return aborted;
  }

  public boolean isFilterEnabled() {
    return filterEnabled;
  }

  public void setBlue(boolean blue) {
    this.blue = blue;
  }

  public void setRed(boolean red) {
    this.red = red;
  }

  public void setYellow(boolean yellow) {
    this.yellow = yellow;
  }

  public void setAborted(boolean aborted) {
    this.aborted = aborted;
  }

  public void setFilterEnabled(boolean filterEnabled) {
    this.filterEnabled = filterEnabled;
  }

  public Width getWidth() {
    return width;
  }

  public void setWidth(Width width) {
    this.width = width;
  }

  public Positioning getAlignment() {
    return alignment;
  }

  public void setAlignment(Positioning alignment) {
    this.alignment = alignment;
  }

  // Invoked in main.jelly to display all the jobs
  public List<RemoteJob> getJobs() {

    String xmlApiUrl = remoteURL + "api/xml";
    try {
      URL url = new URL(xmlApiUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      // Set the username and password for basic authentication
      String authString = UserName + ":" + Password;
      String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());
      String authHeader = "Basic " + encodedAuthString;
      connection.setRequestProperty("Authorization", authHeader);
      Document dom = new SAXReader().read(connection.getInputStream());
      // scan through the job list and print its status
      remoteJobs = new ArrayList<RemoteJob>();
      for (Element job : (List<Element>) dom.getRootElement().elements("job")) {
        RemoteJob r = new RemoteJob(job.elementText("name"), job.elementText("color"), job.elementText("url"));
        String concat = String.valueOf(counter).concat(r.getName());

        /*
         * Filter for the status of the jobs We need a filterEnabled variable here because our normal filter does not
         * cover all possible status and hence it would not display all jobs when completely enabled
         */
        if (filterEnabled) {
          if (r.getStatus().equals("blue") && blue || r.getStatus().equals("yellow") && yellow
              || r.getStatus().equals("red") && red || r.getStatus().equals("aborted") && aborted) {

            if (displayJobs.get(concat).getStatus()) {
              remoteJobs.add(r);
            }
          }
        } else {
          if (displayJobs.get(concat).getStatus()) {
            remoteJobs.add(r);
          }
        }
      }
    } catch (Exception ex) {
      SectionedViewSection.logger.log(Level.SEVERE,
          "XML from remote API " + xmlApiUrl + " looks strange.\n" + ex.getMessage());
    }
    return remoteJobs;
  }

  public List<RemoteJob> getAllJobs() {
    String xmlApiUrl = remoteURL + "api/xml";
    try {
      URL url = new URL(xmlApiUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      // Set the username and password for basic authentication
      String authString = UserName + ":" + Password;
      String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());
      String authHeader = "Basic " + encodedAuthString;
      connection.setRequestProperty("Authorization", authHeader);

      Document dom = new SAXReader().read(connection.getInputStream());
      // scan through the job list
      remoteJobs = new ArrayList<RemoteJob>();
      for (Element job : (List<Element>) dom.getRootElement().elements("job")) {
        RemoteJob r = new RemoteJob(job.elementText("name"), job.elementText("color"), job.elementText("url"));
        remoteJobs.add(r);
      }
    } catch (Exception ex) {
      SectionedViewSection.logger.log(Level.SEVERE,
          "XML from remote API " + xmlApiUrl + " looks strange.\n" + ex.getMessage());
    }
    return remoteJobs;
  }
  @JavaScriptMethod
  public List<RemoteJob> getJobsJS(String l_remoteURL, String username, String password) {
    List<RemoteJob> l_remoteJobs = new ArrayList<RemoteJob>();
    URL url = null;
    try {
      url = new URL(l_remoteURL);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      // Set the username and password for basic authentication
      String authString = username + ":" + password;
      String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());
      String authHeader = "Basic " + encodedAuthString;
      connection.setRequestProperty("Authorization", authHeader);

      SAXReader reader = new SAXReader();
      Document dom = reader.read(connection.getInputStream());
      // scan through the job list and print its status
      for (Element job : (List<Element>) dom.getRootElement().elements("job")) {
        RemoteJob r = new RemoteJob(job.elementText("name"), job.elementText("color"), job.elementText("url"));
        l_remoteJobs.add(r);
      }
    } catch (Exception ex) {
      SectionedViewSection.logger.log(Level.SEVERE,
          "XML from remote API " + url + " looks strange.\n" + ex.getMessage());
    }
    return l_remoteJobs;
  }

  private void determineCss() {
    final StringBuffer css = new StringBuffer();
    css.append(width.getCss());
    css.append(alignment.getCss());
    if (width == Width.FULL || alignment == Positioning.CENTER) {
      css.append("clear: both; ");
    } else if (alignment == Positioning.LEFT) {
      css.append("clear: left; ");
    } else if (alignment == Positioning.RIGHT) {
      css.append("clear: right; ");
    }
    this.css = css.toString();
  }

  // We do not use this method but it has to be there
  public Collection<TopLevelItem> getItems(ItemGroup<? extends TopLevelItem> itemGroup) {
    List<TopLevelItem> items = new ArrayList<TopLevelItem>(1);
    return items;
  }

  public SectionedViewSectionDescriptor getDescriptor() {
    return (SectionedViewSectionDescriptor) Jenkins.getInstance().getDescriptor(getClass());
  }

  public String getCss() {
    return css;
  }

  /**
   * Constants that control how a Section is positioned.
   */
  public enum Positioning {
    CENTER("Center", "margin-left: auto; margin-right: auto; "), LEFT("Left", "float: left; "), RIGHT("Right",
        "float: right; ");

    private final String description;

    private final String css;

    public String getDescription() {
      return description;
    }

    public String getCss() {
      return css;
    }

    public String getName() {
      return name();
    }

    Positioning(String description, String css) {
      this.description = description;
      this.css = css;
    }

    static {
      Stapler.CONVERT_UTILS.register(new EnumConverter(), Positioning.class);
    }
  }

  /**
   * Constants that control how a Section is floated.
   **/

  public enum Width {
    FULL("Full", 100, "width: 100%; "), HALF("1/2", 50, "width: 50%; "), THIRD("1/3", 33, "width: 33%; "), TWO_THIRDS(
        "2/3", 66, "width: 66%; ");

    private final String description;

    private final String css;

    private final int percent;

    public String getDescription() {
      return description;
    }

    public String getCss() {
      return css;
    }

    public int getPercent() {
      return percent;
    }

    public String getName() {
      return name();
    }

    Width(String description, int percent, String css) {
      this.description = description;
      this.percent = percent;
      this.css = css;
    }

    static {
      Stapler.CONVERT_UTILS.register(new EnumConverter(), Width.class);
    }
  }

  public void doSearchSubmit(StaplerRequest request, StaplerResponse response, @QueryParameter("searchTerm") String searchTerm) throws IOException {
    List<RemoteJob> matchingJobs = displayMatchingJobs(searchTerm);
    request.setAttribute("matchingJobs", matchingJobs);
    try {
      System.out.println("jobs to display out changes");
      response.sendRedirect(".");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<RemoteJob> displayMatchingJobs(String searchTerm) {
    System.out.println("display remote jobs");
    List<RemoteJob> matchingJobs = new ArrayList<>();
    RemoteJobsView remoteJobsView = new RemoteJobsView(name);
    Iterable<SectionedViewSection> sectionedViewSections = remoteJobsView.getSections();
    for(SectionedViewSection sectionedViewSection : sectionedViewSections){
      List<RemoteJob> Jobs = sectionedViewSection.getJobs();
      for (RemoteJob remoteJob : remoteJobs) {
        if (remoteJob.getName().contains(searchTerm)) {
          matchingJobs.add(remoteJob);
        }
      }
    }
    return matchingJobs;
  }
}
