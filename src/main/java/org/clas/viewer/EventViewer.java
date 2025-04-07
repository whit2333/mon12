package org.clas.viewer;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.clas.detectors.*;
import org.jlab.detector.decode.CLASDecoder4;
import org.jlab.detector.view.DetectorListener;
import org.jlab.detector.view.DetectorShape2D;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.io.task.DataSourceProcessorPane;
import org.jlab.io.task.IDataEventListener;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.utils.system.ClasUtilsFile;
import org.jlab.jlog.LogEntry; 
import org.jlab.utils.benchmark.BenchmarkTimer;
import org.jlab.utils.options.OptionParser;

        
/**
 *
 * @author ziegler
 * @author devita
 */

public class EventViewer implements IDataEventListener, DetectorListener, ActionListener, ChangeListener {
    
    JTabbedPane tabbedpane = null;
    JPanel mainPanel = null;
    JMenuBar menuBar = null;
    JTextPane clas12Textinfo = new JTextPane();
    DataSourceProcessorPane processorPane = null;
    EmbeddedCanvasTabbed CLAS12Canvas = null;
    private final SchemaFactory schemaFactory = new SchemaFactory();
    CLASDecoder4 clasDecoder = new CLASDecoder4(); 

    private int canvasUpdateTime = 2000;
    private final int analysisUpdateTime = 100;
    private int runNumber = 2284;
    private int ccdbRunNumber = 0;
    private int eventCounter = 0;
    private int histoResetEvents = 0;
    BenchmarkTimer timer = new BenchmarkTimer("mon12");

    private String defaultEtHost = null;
    private String defaultEtIp = null;
    
    public String outputDirectory = null; 
    public String logbookName = null;
    private long triggerMask;
    private boolean autoSave;

    public LinkedHashMap<String, DetectorMonitor> monitors = new LinkedHashMap<>();
    
    public BeamMonitor beamMonitor = null;

    public final void initMonitors() {
        this.monitors.put("AHDC",        new AHDCmonitor("AHDC"));
        this.monitors.put("ATOF",        new ATOFmonitor("ATOF"));
        this.monitors.put("ALERT",       new ALERTMonitor("ALERT"));
        this.monitors.put("BAND",        new BANDmonitor("BAND"));
        this.monitors.put("BMT",         new BMTmonitor("BMT"));
        this.monitors.put("BST",         new BSTmonitor("BST"));
        this.monitors.put("CND",         new CNDmonitor("CND")); 
        this.monitors.put("CTOF",        new CTOFmonitor("CTOF")); 
        this.monitors.put("DC",          new DCmonitor("DC"));     
        this.monitors.put("ECAL",        new ECmonitor("ECAL"));       
        this.monitors.put("FMT",         new FMTmonitor("FMT"));      
        this.monitors.put("FTCAL",       new FTCALmonitor("FTCAL"));   
        this.monitors.put("FTHODO",      new FTHODOmonitor("FTHODO")); 
        this.monitors.put("FTOF",        new FTOFmonitor("FTOF"));             
        this.monitors.put("FTTRK",       new FTTRKmonitor("FTTRK"));   
        this.monitors.put("HTCC",        new HTCCmonitor("HTCC"));     
        this.monitors.put("LTCC",        new LTCCmonitor("LTCC")); 
        this.monitors.put("RASTER",      new RASTERmonitor("RASTER"));    
        this.monitors.put("RICH",        new RICHmonitor("RICH"));    
        this.monitors.put("RTPC",        new RTPCmonitor("RTPC"));    
        this.monitors.put("RF",          new RFmonitor("RF"));       
        this.monitors.put("HEL",         new HELmonitor("HEL"));      
        this.monitors.put("FCUP",        new FCUPmonitor("FCUP")); 
        this.monitors.put("Trigger",     new TRIGGERmonitor("Trigger"));
        this.monitors.put("TimeJitter",  new TJITTERmonitor("TimeJitter"));
    }
                    
    public EventViewer(String host, String ip) {
        this.mainPanel = new JPanel();	
        this.mainPanel.setLayout(new BorderLayout());
        this.tabbedpane = new JTabbedPane();
        this.tabbedpane.addChangeListener(this);
        this.defaultEtHost = host;
        this.defaultEtIp   = ip;
        this.processorPane = new DataSourceProcessorPane(defaultEtHost, defaultEtIp);
        this.processorPane.setUpdateRate(analysisUpdateTime);
        this.processorPane.addEventListener(this);
        this.mainPanel.add(tabbedpane);
        this.mainPanel.add(processorPane,BorderLayout.PAGE_END);
        this.initMonitors();
    }
    
    public void init() {
        this.initsPaths();
        this.initSummary();
        this.initTabs();
        this.initMenus();
        this.initLoggers();
    }
    
    public void initLoggers() {
        Logger.getLogger("org.freehep.math.minuit").setLevel(Level.WARNING);
    }
    
    public void initMenus() {   
        this.menuBar = new JMenuBar();
        JMenuItem menuItem;
        JMenu file = new JMenu("File");
        file.getAccessibleContext().setAccessibleDescription("File options");
        menuItem = new JMenuItem("Read histograms from file");
        menuItem.getAccessibleContext().setAccessibleDescription("Read histograms from file");
        menuItem.addActionListener(this);
        file.add(menuItem);
        menuItem = new JMenuItem("Save histograms to file");
        menuItem.getAccessibleContext().setAccessibleDescription("Save histograms to file");
        menuItem.addActionListener(this);
        file.add(menuItem);
        menuItem = new JMenuItem("Print histograms as png");
        menuItem.getAccessibleContext().setAccessibleDescription("Print histograms as png");
        menuItem.addActionListener(this);
        file.add(menuItem);
        this.menuBar.add(file);

        JMenu settings = new JMenu("Settings");
        settings.getAccessibleContext().setAccessibleDescription("Choose monitoring parameters");
        menuItem = new JMenuItem("Set GUI update interval");
        menuItem.getAccessibleContext().setAccessibleDescription("Set GUI update interval");
        menuItem.addActionListener(this);
        settings.add(menuItem);
        menuItem = new JMenuItem("Set global z-axis log scale");
        menuItem.getAccessibleContext().setAccessibleDescription("Set global z-axis log scale");
        menuItem.addActionListener(this);
        settings.add(menuItem);
        menuItem = new JMenuItem("Set global z-axis lin scale");
        menuItem.getAccessibleContext().setAccessibleDescription("Set global z-axis lin scale");
        menuItem.addActionListener(this);
        settings.add(menuItem);
        menuItem = new JMenuItem("Set DC occupancy scale max");
        menuItem.getAccessibleContext().setAccessibleDescription("Set DC occupancy scale max");
        menuItem.addActionListener(this);
        settings.add(menuItem);
        menuItem = new JMenuItem("Set DC ToT threshold");
        menuItem.getAccessibleContext().setAccessibleDescription("Set DC ToT threshold");
        menuItem.addActionListener(this);
        settings.add(menuItem);
        menuItem = new JMenuItem("Set run number");
        menuItem.getAccessibleContext().setAccessibleDescription("Set run number");
        menuItem.addActionListener(this);
        settings.add(menuItem);
        this.menuBar.add(settings);
         
        JMenu upload = new JMenu("Upload");
        upload.getAccessibleContext().setAccessibleDescription("Upload histograms to the Logbook");
        menuItem = new JMenuItem("Upload all histos to the logbook");
        menuItem.getAccessibleContext().setAccessibleDescription("Upload all histos to the logbook");
        menuItem.addActionListener(this);
        upload.add(menuItem);
        this.menuBar.add(upload);
        
        JMenu reset = new JMenu("Reset");
        reset.getAccessibleContext().setAccessibleDescription("Reset histograms");        
        JMenuItem menuItemdisable = new JMenuItem("Set periodic reset");
        menuItemdisable.getAccessibleContext().setAccessibleDescription("Set periodic reset");
        menuItemdisable.addActionListener(this);
        reset.add(menuItemdisable);        
        for(String key : this.monitors.keySet()) {
            if(this.monitors.get(key).isActive()) {
                JMenuItem menuItemDet = new JMenuItem("Reset " + key + " histograms");
                menuItemDet.getAccessibleContext().setAccessibleDescription("Reset " + key + " histograms");
                menuItemDet.addActionListener(this);
                reset.add(menuItemDet);
            }
        }        
        JMenuItem menuItemStream = new JMenuItem("Reset stdout/stderr");
        menuItemStream.getAccessibleContext().setAccessibleDescription("Reset stdout/stderr");
        menuItemStream.addActionListener(this);
        reset.add(menuItemStream);        
        this.menuBar.add(reset);

        String[] triggers = { "Electron OR", "Electron Sec 1","Electron Sec 2","Electron Sec 3",
                        "Electron Sec 4","Electron Sec 5","Electron Sec 6",
		        "","","","","","","","","","","","","","","","","","","","","","","","",
		        "Random Pulser"};    
        
        JMenu trigBitsBeam = new JMenu("TriggerBits");
        trigBitsBeam.getAccessibleContext().setAccessibleDescription("Select Trigger Bits");        
        for (int i=0; i<triggers.length; i++) {
            JCheckBoxMenuItem bb = new JCheckBoxMenuItem(i + " " + triggers[i]);
            final int bit = i;
            bb.addItemListener(new ItemListener() {
                @Override
                @SuppressWarnings("empty-statement")
                public void itemStateChanged(ItemEvent e) {
                    if(e.getStateChange() == ItemEvent.SELECTED) {
                        for(String key : monitors.keySet()) {
                            monitors.get(key).setUITriggerMask(bit);
                        }
                    } else {
                        for(String key : monitors.keySet()) {
                            monitors.get(key).clearUITriggerMask(bit);
                        }
                    };
                }
            });
            boolean bstate = ((this.triggerMask >> i) & 1) == 1;
            bb.setState(bstate);
            trigBitsBeam.add(bb); 
        	        	
        }
        menuBar.add(trigBitsBeam); 
    }

    public void initsPaths() {
        String dir = ClasUtilsFile.getResourceDir("CLAS12DIR", "etc/bankdefs/hipo4");
        this.schemaFactory.initFromDirectory(dir);
        if (this.outputDirectory == null) {
            this.outputDirectory = System.getProperty("user.home") + "/CLAS12MON/output";
        }
        System.out.println("Output directory set to: " + this.outputDirectory);
    }

    public void initSummary() {
        GStyle.getAxisAttributesX().setTitleFontSize(18);
        GStyle.getAxisAttributesX().setLabelFontSize(14);
        GStyle.getAxisAttributesY().setTitleFontSize(18);
        GStyle.getAxisAttributesY().setLabelFontSize(14);
        
        if(this.monitors.get("DC").isActive() ||
           this.monitors.get("HTCC").isActive() ||
           this.monitors.get("LTCC").isActive() ||
           this.monitors.get("FTOF").isActive() ||
           this.monitors.get("ECAL").isActive() ||
           this.monitors.get("RICH").isActive()) {
           this.createSummary("FD",3,3);
        }
        if(this.monitors.get("BST").isActive() ||
           this.monitors.get("BMT").isActive() ||
           this.monitors.get("CTOF").isActive() ||
           this.monitors.get("CND").isActive()) {
           this.createSummary("CD",2,2);
        }
        if(this.monitors.get("AHDC").isActive() ||
           this.monitors.get("ATOF").isActive() ||
           this.monitors.get("ALERT").isActive()) {
           this.createSummary("ALERT",1,2);
        }
        if(this.monitors.get("FTCAL").isActive() ||
           this.monitors.get("FTHODO").isActive() ||
           this.monitors.get("FTTRK").isActive()) {
           this.createSummary("FT",1,3);
        }
        if(this.monitors.get("RF").isActive() ||
           this.monitors.get("HEL").isActive() ||
           this.monitors.get("Trigger").isActive() ||
           this.monitors.get("TimeJitter").isActive()) {
           this.createSummary("RF/HEL/JITTER/TRIGGER",2,2);
        }
        
        JPanel CLAS12View = new JPanel(new BorderLayout());
        JSplitPane splitPanel = new JSplitPane();
        splitPanel.setLeftComponent(CLAS12View);
        splitPanel.setRightComponent(this.CLAS12Canvas);
        JTextPane clas12Text   = new JTextPane();
        clas12Text.setText("CLAS12\n monitoring plots\n V7.11\n");
        clas12Text.setEditable(false);       
        this.clas12Textinfo.setEditable(false);
        this.clas12Textinfo.setFont(new Font("Avenir",Font.PLAIN,16));
        this.clas12Textinfo.setBackground(CLAS12View.getBackground());
        StyledDocument styledDoc = clas12Text.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        styledDoc.setParagraphAttributes(0, styledDoc.getLength(), center, false);
        clas12Text.setBackground(CLAS12View.getBackground());
        clas12Text.setFont(new Font("Avenir",Font.PLAIN,20));
        JLabel clas12Design = this.getImage("https://www.jlab.org/Hall-B/clas12-web/sidebar/clas12-design.jpg",0.08);
        CLAS12View.add(this.clas12Textinfo,BorderLayout.BEFORE_FIRST_LINE );
        CLAS12View.add(clas12Design);
        CLAS12View.add(clas12Text,BorderLayout.PAGE_END);

        this.tabbedpane.add(splitPanel,"Summary");
    }
    
    public void createSummary(String name, int nx, int ny) {
        if(this.CLAS12Canvas==null) this.CLAS12Canvas = new EmbeddedCanvasTabbed(name);
        else                   this.CLAS12Canvas.addCanvas(name);
        this.CLAS12Canvas.getCanvas(name).divide(nx,ny);
        this.CLAS12Canvas.getCanvas(name).setGridX(false);
        this.CLAS12Canvas.getCanvas(name).setGridY(false); 
    }
    public void initTabs() {
        this.plotSummaries();
        for(String key : this.monitors.keySet()) {
            if(this.monitors.get(key).isActive())
                this.tabbedpane.add(this.monitors.get(key).getDetectorPanel(), this.monitors.get(key).getDetectorName()); //don't show FMT tab
            this.monitors.get(key).getDetectorView().getView().addDetectorListener(this);                       
        }
        this.tabbedpane.add(new Acronyms(),"Acronyms");
        this.setCanvasUpdate(this.canvasUpdateTime);
     }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e.getActionCommand());
        if("Set GUI update interval".equals(e.getActionCommand())) {
            this.chooseUpdateInterval();
        }
        if("Set global z-axis log scale".equals(e.getActionCommand())) {
            for(String key : monitors.keySet()) {
                this.monitors.get(key).setLogZ(true);this.monitors.get(key).plotHistos();
            }
        }
        if("Set global z-axis lin scale".equals(e.getActionCommand())) {
           for(String key : monitors.keySet()) {
               this.monitors.get(key).setLogZ(false);this.monitors.get(key).plotHistos();
           }
        }
        if("Set DC occupancy scale max".equals(e.getActionCommand())) {
           this.setDCRange(e.getActionCommand());
        }
        if("Set DC ToT threshold".equals(e.getActionCommand())) {
           this.setDCToTThreshold(e.getActionCommand());
        }
        if("Set run number".equals(e.getActionCommand())) {
           this.setRunNumber(e.getActionCommand());
        }
        if("Read histograms from file".equals(e.getActionCommand())) {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            File workingDirectory = new File(System.getProperty("user.dir"));  
            fc.setCurrentDirectory(workingDirectory);
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                String fileName = fc.getSelectedFile().getAbsolutePath();
                this.loadHistosFromFile(fileName);
            }
        }        
        if("Save histograms to file".equals(e.getActionCommand())) {
            DateFormat df = new SimpleDateFormat("MM-dd-yyyy_hh.mm.ss_aa");
            String fileName = "CLAS12Mon_run_" + this.runNumber + "_" + df.format(new Date()) + ".hipo";
            JFileChooser fc = new JFileChooser();
            File workingDirectory = new File(System.getProperty("user.dir"));   
            fc.setCurrentDirectory(workingDirectory);
            File file = new File(fileName);
            fc.setSelectedFile(file);
            if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
               fileName = fc.getSelectedFile().getAbsolutePath();
               this.saveHistosToFile(fileName);
            }
        }
        if("Print histograms as png".equals(e.getActionCommand())) {
            this.saveAllImages(true, false);
        }
        if("Upload all histos to the logbook".equals(e.getActionCommand())) {   
            Map<String,String> images = this.saveAllImages(true, true);
            LogEntry entry = new LogEntry("All online monitoring histograms for run number " + this.runNumber, this.logbookName);
            System.out.println("Starting to upload all monitoring plots");
            try {
                for (String path : images.keySet()) {
                    entry.addAttachment(path, images.get(path));
                }
                long lognumber = entry.submitNow();
              System.out.println("Successfully submitted log entry number: " + lognumber); 
            } catch(Exception exc){
                exc.printStackTrace(); 
                System.out.println( exc.getMessage());
            }
        }    
        if ("Set periodic reset".equals(e.getActionCommand())){
            this.choosePeriodicReset();
        }
        if ( e.getActionCommand().substring(0, 5).equals("Reset")
                && e.getActionCommand().split(" ").length==3){
            String key = e.getActionCommand().split(" ")[1];
            if(this.monitors.containsKey(key)) this.monitors.get(key).resetEventListener();
        }
        if ("Reset stdout/stderr".equals(e.getActionCommand())){
            DetectorMonitor.resetStreams();
        }
    }

    /**
     * @param png whether to save png files
     * @param hipo whether to save hipo file
     * @return image Path,Title pairs 
     */
    public Map<String,String> saveAllImages(boolean png, boolean hipo) {
        System.out.println("\n******* Saving All Images ....\n");
        Map<String,String> ret = new LinkedHashMap<>();
        DateFormat df = new SimpleDateFormat("MM-dd-yyyy_hh.mm.ss_aa");
        String tstamp = df.format(new Date());
        String dir = this.outputDirectory + "/clas12mon_" + this.runNumber + "_" + tstamp;
        File directory = new File(dir);
        if (!directory.exists()) directory.mkdirs();
        if (hipo) {
            try{
                this.saveHistosToFile(dir + "/clas12mon_histos_" + this.runNumber + "_" + tstamp + ".hipo"); 
            }
            catch(IndexOutOfBoundsException e){
                e.printStackTrace(); 
                System.err.println( e.getMessage());
            }
        }
        if (png) {
            if (this.CLAS12Canvas.getCanvas("FD") != null) {
                String fileName = dir + "/summary_FD_" + tstamp + ".png";
                this.CLAS12Canvas.getCanvas("FD").save(fileName);
                ret.put(fileName, "Summary plots for the forward detector");
            }
            if (this.CLAS12Canvas.getCanvas("CD") != null) {
                String fileName = dir + "/summary_CD_" + tstamp + ".png";
                this.CLAS12Canvas.getCanvas("CD").save(fileName);
                ret.put(fileName, "Summary plots for the central detector");
            }
            if (this.CLAS12Canvas.getCanvas("ALERT") != null) {
                String fileName = dir + "/summary_ALERT_" + tstamp + ".png";
                this.CLAS12Canvas.getCanvas("ALERT").save(fileName);
                ret.put(fileName, "Summary plots for the ALERT detector");
            }
            if (this.CLAS12Canvas.getCanvas("FT") != null) {
                String fileName = dir + "/summary_FT_" + tstamp + ".png";
                this.CLAS12Canvas.getCanvas("FT").save(fileName);
                ret.put(fileName, "Summary plots for the forward tagger");
            }
            if (this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER") != null) {
                String fileName = dir + "/summary_RHJT_" + tstamp + ".png";
                this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER").save(fileName);
                ret.put(fileName, "Summary plots RF/HEL/JITTER/TRIGGER");
            }
            for (String key : this.monitors.keySet()) {
                if (this.monitors.get(key).isActive()) {
                    ret.putAll(this.monitors.get(key).printCanvas(dir));
                }
            }
        }
        for (String path : ret.keySet()) System.out.println("Saved "+path);
        return ret;
    }

    public void choosePeriodicReset() {
        String s = (String)JOptionPane.showInputDialog(
                    null,
                    "Set periodic histogram reset (#events), 0-disabled ",
                    " ",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "0");
        if(s!=null){
            int nev = 1000;
            try { 
                nev= Integer.parseInt(s);
            } catch(NumberFormatException e) { 
                JOptionPane.showMessageDialog(null, "Value must be a >=0!");
            }
            if(nev>=0) {
                this.histoResetEvents = nev;
                System.out.println("Resetting histograms every " + this.histoResetEvents + " events");
            }
            else {
                JOptionPane.showMessageDialog(null, "Value must be a >=0!");
            }
        }
    }
        
    public void chooseUpdateInterval() {
        String s = (String)JOptionPane.showInputDialog(
                    null,
                    "GUI update interval (ms)",
                    " ",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "1000");
        if(s!=null){
            int time = 1000;
            try { 
                time= Integer.parseInt(s);
            } catch(NumberFormatException e) { 
                JOptionPane.showMessageDialog(null, "Value must be a positive integer!");
            }
            if(time>0) {
                this.setCanvasUpdate(time);
            }
            else {
                JOptionPane.showMessageDialog(null, "Value must be a positive integer!");
            }
        }
    }
        
    private JLabel getImage(String path, double scale) {
        Image image = null;
        try {
            URL url = new URL(path);
            image = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Picture upload from " + path + " failed");
        }
        ImageIcon imageIcon = new ImageIcon(image);
        double width = imageIcon.getIconWidth() * scale;
        double height = imageIcon.getIconHeight() * scale;
        imageIcon = new ImageIcon(image.getScaledInstance((int) width, (int) height, Image.SCALE_SMOOTH));
        return new JLabel(imageIcon);
    }

    private int getEventNumber(DataEvent event) {
        DataBank bank = event.getBank("RUN::config");
        return bank != null ? bank.getInt("event", 0): this.eventCounter;
    }

    private void copyHitList(String k, String mon1, String mon2) {
    	if (k == null ? mon1 != null : !k.equals(mon1)) return;
    	this.monitors.get(mon1).ttdcs = this.monitors.get(mon2).ttdcs;
    	this.monitors.get(mon1).ftdcs = this.monitors.get(mon2).ftdcs;
    	this.monitors.get(mon1).fadcs = this.monitors.get(mon2).fadcs;
   	this.monitors.get(mon1).fapmt = this.monitors.get(mon2).fapmt;
    	this.monitors.get(mon1).ftpmt = this.monitors.get(mon2).ftpmt;
    }
    
    
    @Override
    public void dataEventAction(DataEvent event) {
        
        this.timer.resume();
        if (event == null) return;

        // check beam current:
        if (beamMonitor != null && !beamMonitor.getBeamStatus()) {
            return;
        }
        
        // convert event to HIPO:
    	DataEvent hipo = event;
        if (event instanceof EvioDataEvent) {
            Event dump = this.clasDecoder.getDataEvent(event);
            Bank header = this.clasDecoder.createHeaderBank(this.ccdbRunNumber, getEventNumber(event), (float) 0, (float) 0);
            Bank trigger = this.clasDecoder.createTriggerBank();
            Bank helicity = this.clasDecoder.createHelicityDecoderBank((EvioDataEvent)event);
            Bank onlineHelicity = this.clasDecoder.createOnlineHelicityBank();
            if(onlineHelicity!=null) dump.write(onlineHelicity);
            if (header != null) dump.write(header);
            if (trigger != null) dump.write(trigger);
            if (helicity != null) dump.write(helicity);
            this.clasDecoder.extractPulses(dump);  // Apply AHDC Decoder !!!
	    hipo = new HipoDataEvent(dump, this.schemaFactory);
        }
        
        // if header bank is missing, do nothing
        if(!hipo.hasBank("RUN::config")) {
            return;
        }
        DataBank config = hipo.getBank("RUN::config");
        int run  = config.getInt("run", 0);
        int ev   = config.getInt("event", 0);
        long tg  = config.getLong("trigger", 0);
        long ts  = config.getLong("timestamp", 0);
        
        // if run number is invalid, do nothibg
        if(run<1) {
            return;
        }

        // propagate header information
        for(String key : monitors.keySet()) {
            if(this.monitors.get(key).isActive()) {
                this.monitors.get(key).setRunNumber(run);
                this.monitors.get(key).setEventNumber(ev);
                this.monitors.get(key).setTriggerWord(tg);
                this.monitors.get(key).setTimeStamp(ts);
            }
        } 

        // if run number changes, reset monitors
        if(run!=this.runNumber) {
            System.out.println("\nSetting run number to: " + run + "\n");
            this.resetEventListener();
            this.runNumber = run;
            this.clas12Textinfo.setText("\nrun number: " + this.runNumber + "\n");
        }

        // only count events if the trigger mask is satisfied:
        if (this.triggerMask==0L || 
           (tg & this.triggerMask) != 0L) this.eventCounter++;

        // periodically, automatically reset histograms and save images:
        if (this.histoResetEvents > 0 && this.eventCounter > this.histoResetEvents) {
            if (this.autoSave) this.saveAllImages(true, false);
            this.resetEventListener();
        }
      
        // finally, fill the histograms:
        for (String key : monitors.keySet()) {
            if (this.monitors.get(key).isActive()) {
                 copyHitList(key, "Trigger", "FTOF");
                this.monitors.get(key).dataEventAction(hipo);
            }
        }
    }

    @Override
    public void resetEventListener() {
        System.out.println("\n******* Zeroing histograms and event counter.\n");
        this.timer.pause();
        System.out.println(this.timer);
        this.eventCounter = 0;
        for(String key : monitors.keySet()) {
            if(this.monitors.get(key).isActive()) {
                this.monitors.get(key).resetEventListener();
                this.monitors.get(key).timerUpdate();
            }
        }      
        this.plotSummaries();
    }

    public void loadHistosFromFile(String fileName) {
        System.out.println("Opening file: " + fileName);
        TDirectory dir = new TDirectory();
        dir.readFile(fileName);
        dir.cd();
        dir.pwd();
        for(String key : this.monitors.keySet())
            this.monitors.get(key).readDataGroup(dir);
        this.plotSummaries();
    }

    public void plotSummaries() {
        
        /// FD:
        if(this.CLAS12Canvas!=null && this.CLAS12Canvas.getCanvas("FD")!=null) {
            // DC
            this.CLAS12Canvas.getCanvas("FD").cd(0);
            if(this.monitors.get("DC").isActive() && this.monitors.get("DC").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("DC").getDetectorSummary().getH1F("summary")); 
            // HTTC
            this.CLAS12Canvas.getCanvas("FD").cd(1);
            if(this.monitors.get("HTCC").isActive() && this.monitors.get("HTCC").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("HTCC").getDetectorSummary().getH1F("summary"));
            // LTTC
            this.CLAS12Canvas.getCanvas("FD").cd(2);
            if(this.monitors.get("LTCC").isActive() && this.monitors.get("LTCC").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("LTCC").getDetectorSummary().getH1F("summary"));
            // RICH
            this.CLAS12Canvas.getCanvas("FD").cd(3);
            this.CLAS12Canvas.getCanvas("FD").getPad(3).getAxisY().setLog(true);
            if(this.monitors.get("RICH").isActive() && this.monitors.get("RICH").getDetectorSummary()!=null) {
                this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("RICH").getDetectorSummary().getH1F("summary_1"));
                this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("RICH").getDetectorSummary().getH1F("summary_4"), "same");
            }

            // ECAL 
            this.CLAS12Canvas.getCanvas("FD").cd(4); this.CLAS12Canvas.getCanvas("FD").getPad(4).setAxisRange(0.5,6.5,0.5,1.5);
            if(this.monitors.get("ECAL").isActive() && this.monitors.get("ECAL").getDetectorSummary()!=null) { 
                    this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("ECAL").getDetectorSummary().getGraph("PCALu"));
                    this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("ECAL").getDetectorSummary().getGraph("PCALv"),"same");
                    this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("ECAL").getDetectorSummary().getGraph("PCALw"),"same");
                    this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("ECAL").getDetectorSummary().getF1D("p0"),"same");
            }
            this.CLAS12Canvas.getCanvas("FD").cd(5); this.CLAS12Canvas.getCanvas("FD").getPad(5).setAxisRange(0.5,6.5,0.5,1.5);
            if(this.monitors.get("ECAL").isActive() && this.monitors.get("ECAL").getDetectorSummary()!=null) {
                    this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("ECAL").getDetectorSummary().getGraph("ECinu"));
                    this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("ECAL").getDetectorSummary().getGraph("ECinv"),"same");
                    this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("ECAL").getDetectorSummary().getGraph("ECinw"),"same");
                    this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("ECAL").getDetectorSummary().getGraph("ECoutu"),"same");
                    this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("ECAL").getDetectorSummary().getGraph("ECoutv"),"same");
                    this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("ECAL").getDetectorSummary().getGraph("ECoutw"),"same");
                    this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("ECAL").getDetectorSummary().getF1D("p0"),"same");
            }
            // FTOF:
            this.CLAS12Canvas.getCanvas("FD").cd(6);
            this.CLAS12Canvas.getCanvas("FD").getPad(6).getAxisZ().setLog(true);
            if(this.monitors.get("FTOF").isActive() && this.monitors.get("FTOF").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("FTOF").getDetectorSummary().getH1F("sum_p1"));
            this.CLAS12Canvas.getCanvas("FD").cd(7);
            this.CLAS12Canvas.getCanvas("FD").getPad(7).getAxisZ().setLog(true);
            if(this.monitors.get("FTOF").isActive() && this.monitors.get("FTOF").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("FTOF").getDetectorSummary().getH1F("sum_p2"));
            this.CLAS12Canvas.getCanvas("FD").cd(8);
            this.CLAS12Canvas.getCanvas("FD").getPad(8).getAxisZ().setLog(true);
            if(this.monitors.get("FTOF").isActive() && this.monitors.get("FTOF").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("FD").draw(this.monitors.get("FTOF").getDetectorSummary().getH1F("sum_p3"));
        }
        
        ///  CD:
        if(this.CLAS12Canvas!=null && this.CLAS12Canvas.getCanvas("CD")!=null) {
            // CND
            this.CLAS12Canvas.getCanvas("CD").cd(0);
            if(this.monitors.get("CND").isActive() && this.monitors.get("CND").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("CD").draw(this.monitors.get("CND").getDetectorSummary().getH1F("summary"));
            // CTOF
            this.CLAS12Canvas.getCanvas("CD").cd(1);
            if(this.monitors.get("CTOF").isActive() && this.monitors.get("CTOF").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("CD").draw(this.monitors.get("CTOF").getDetectorSummary().getH1F("summary"));
            // BMT
            this.CLAS12Canvas.getCanvas("CD").cd(2);
            this.CLAS12Canvas.getCanvas("CD").getPad(2).getAxisZ().setLog(true);
            if(this.monitors.get("BMT").isActive() && this.monitors.get("BMT").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("CD").draw(this.monitors.get("BMT").getDetectorSummary().getH1F("summary"));
            // BST
            this.CLAS12Canvas.getCanvas("CD").cd(3);
            this.CLAS12Canvas.getCanvas("CD").getPad(3).getAxisZ().setLog(true);
            if(this.monitors.get("BST").isActive() && this.monitors.get("BST").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("CD").draw(this.monitors.get("BST").getDetectorSummary().getH2F("summary"));
        }

        // ALERT:
        if(this.CLAS12Canvas!=null && this.CLAS12Canvas.getCanvas("ALERT")!=null) {
            // AHDC
            this.CLAS12Canvas.getCanvas("ALERT").cd(0);
            if(this.monitors.get("AHDC").isActive() && this.monitors.get("AHDC").getDetectorSummary()!=null) {
		this.CLAS12Canvas.getCanvas("ALERT").getPad(0).getAxisZ().setLog(this.monitors.get("AHDC").getLogZ());
                this.CLAS12Canvas.getCanvas("ALERT").draw(this.monitors.get("AHDC").getDetectorSummary().getH2F("summary"));
            }
            // ATOF
            this.CLAS12Canvas.getCanvas("ALERT").cd(1);
            if(this.monitors.get("ATOF").isActive() && this.monitors.get("ATOF").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("ALERT").draw(this.monitors.get("ATOF").getDetectorSummary().getH1F("summary"));
        }
                
        // FT:
        if(this.CLAS12Canvas!=null && this.CLAS12Canvas.getCanvas("FT")!=null) {
            // FTCAL
            this.CLAS12Canvas.getCanvas("FT").cd(0);
            if(this.monitors.get("FTCAL").isActive() && this.monitors.get("FTCAL").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("FT").draw(this.monitors.get("FTCAL").getDetectorSummary().getH1F("summary"));
            // FTHODO
            this.CLAS12Canvas.getCanvas("FT").cd(1);
            if(this.monitors.get("FTHODO").isActive() && this.monitors.get("FTHODO").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("FT").draw(this.monitors.get("FTHODO").getDetectorSummary().getH1F("summary"));
            // FTTRK
            this.CLAS12Canvas.getCanvas("FT").cd(2);
            if(this.monitors.get("FTTRK").isActive() && this.monitors.get("FTTRK").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("FT").draw(this.monitors.get("FTTRK").getDetectorSummary().getH1F("summary"));
        }
      
        // RF/HEL/JITTER/TRIGGER:
        if(this.CLAS12Canvas!=null && this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER")!=null) {
            // RF
            this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER").cd(0);
            if(this.monitors.get("RF").isActive() && this.monitors.get("RF").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER").draw(this.monitors.get("RF").getDetectorSummary().getH1F("summary"));
            // HEL
            this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER").cd(1);
            if(this.monitors.get("HEL").isActive() && this.monitors.get("HEL").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER").draw(this.monitors.get("HEL").getDetectorSummary().getH1F("summary"));
            // FCUP
            this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER").cd(2);
            if(this.monitors.get("TimeJitter").isActive() && this.monitors.get("TimeJitter").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER").draw(this.monitors.get("TimeJitter").getDetectorSummary().getH1F("summary"));
            // TRIGGER
            this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER").cd(3);
            this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER").getPad(3).getAxisY().setLog(true);
            if(this.monitors.get("Trigger").isActive() && this.monitors.get("Trigger").getDetectorSummary()!=null) 
                this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER").draw(this.monitors.get("Trigger").getDetectorSummary().getH1F("summary"));
        }
    }

    public void saveHistosToFile(String fileName) {
        TDirectory dir = new TDirectory();
        for(String key : monitors.keySet()) {
            this.monitors.get(key).writeDataGroup(dir);
        }
        System.out.println("******* Saving histograms to file " + fileName);
        dir.writeFile(fileName);
    }
        
    public void setCanvasUpdate(int time) {
        System.out.println("Setting " + time + " ms update interval");
        this.canvasUpdateTime = time;
        if(this.CLAS12Canvas!=null) {
            if(this.CLAS12Canvas.getCanvas("FD")!=null) {
                this.CLAS12Canvas.getCanvas("FD").initTimer(time);
                this.CLAS12Canvas.getCanvas("FD").update();
            }
            if (this.CLAS12Canvas.getCanvas("CD") != null) {
                this.CLAS12Canvas.getCanvas("CD").initTimer(time);
                this.CLAS12Canvas.getCanvas("CD").update();
            }
            if (this.CLAS12Canvas.getCanvas("ALERT") != null) {
                this.CLAS12Canvas.getCanvas("ALERT").initTimer(time);
                this.CLAS12Canvas.getCanvas("ALERT").update();
            }
            if (this.CLAS12Canvas.getCanvas("FT") != null) {
                this.CLAS12Canvas.getCanvas("FT").initTimer(time);
                this.CLAS12Canvas.getCanvas("FT").update();
            }
            if(this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER")!=null) {
                this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER").initTimer(time);
                this.CLAS12Canvas.getCanvas("RF/HEL/JITTER/TRIGGER").update();
            }
        }
        for(String key : monitors.keySet()) {
            if(this.monitors.get(key).isActive()) this.monitors.get(key).setCanvasUpdate(time);
        }
    }
    
    private void setDCRange(String actionCommand) {
        String dcScale = (String) JOptionPane.showInputDialog(null, "Set normalized DC occuopancy range maximum to ", " ", JOptionPane.PLAIN_MESSAGE, null, null, "15");
        if (dcScale != null) { 
            double dcScaleMax= 0;
            try {dcScaleMax = Double.parseDouble(dcScale);} 
            catch (NumberFormatException f) {JOptionPane.showMessageDialog(null, "Value must be a positive integer!");}
            if (dcScaleMax > 0){ this.monitors.get("DC").max_occ = dcScaleMax;} 
            else {JOptionPane.showMessageDialog(null, "Value must be a positive number!");}   
        }
        System.out.println("Set normalized DC occuopancy range maximum to " + dcScale);
    }
    
    private void setDCToTThreshold(String actionCommand) {
        String dcToT = (String) JOptionPane.showInputDialog(null, "Set DC ToT threshold ", " ", JOptionPane.PLAIN_MESSAGE, null, null, "20");
        if (dcToT != null) { 
            double dcToTmin= 0;
            try {dcToTmin = Double.parseDouble(dcToT);} 
            catch (NumberFormatException f) {JOptionPane.showMessageDialog(null, "Value must be a positive integer!");}
            if (dcToTmin > 0){ this.monitors.get("DC").minToT = dcToTmin;} 
            else {JOptionPane.showMessageDialog(null, "Value must be a positive number!");}   
        }
        System.out.println("Set DC ToT threshold to " + dcToT);
    }
    
    private void setRunNumber(String actionCommand) {
        System.out.println("Set run number for CCDB access");
        String runNumber = (String) JOptionPane.showInputDialog(null, "Set run number to ", " ", JOptionPane.PLAIN_MESSAGE, null, null, "2284");
        if (runNumber != null) { 
            int currentRunNumber= this.runNumber;
            try {
                currentRunNumber = Integer.parseInt(runNumber);
            } 
            catch (
                NumberFormatException f) {JOptionPane.showMessageDialog(null, "Value must be a positive integer!");
            }
            if (currentRunNumber > 0){ 
                this.ccdbRunNumber = currentRunNumber;               
                this.clasDecoder.setRunNumber(currentRunNumber,true);
            } 
            else {JOptionPane.showMessageDialog(null, "Value must be a positive integer!");}   
        }
    }

    public void stateChanged(ChangeEvent e) {
        this.timerUpdate();
    }
    
    @Override
    public void timerUpdate() {
        this.initLoggers();
        for(String key : monitors.keySet()) {
            if(this.monitors.get(key).isActive()) 
                this.monitors.get(key).timerUpdate();
        }
        this.plotSummaries();
   }

    public static void main(String[] args){
        
        OptionParser parser = new OptionParser("mon12");
        parser.setRequiresInputList(false);
        parser.setDescription("CLAS12 monitoring app");
        parser.addOption("-geometry", "1600x1000",      "Select window size, e.g. 1600x1200");
        parser.addOption("-tabs",     "All",            "Select active tabs, e.g. BST:FTOF");
        parser.addOption("-logbook",  "HBLOG",          "Select electronic logbook");
        parser.addOption("-trigger",  "0x0",            "Select trigger bits (0x0 = all)");
        parser.addOption("-ethost",   "clondaq6",       "Select ET host name");
        parser.addOption("-etip",     "129.57.167.60",  "Select ET host IP address");
        parser.addOption("-etsession","/et/clasprod",  "DAQ session, usually clasprod or clastest7");
        parser.addOption("-autosave", "-1",             "Autosave every N events (e.g. for Hydra)");
        parser.addOption("-batch",    "0",              "Connect and run automatically");
        parser.addOption("-outDir",   null,             "Path for output PNG/HIPO files");
        parser.addOption("-current",  "-1",             "Minimum beam current");
        parser.addOption("-variation", "default",       "CCDB variation");
        parser.parse(args);

        EventViewer viewer = new EventViewer(parser.getOption("-ethost").stringValue(),
        parser.getOption("-etip").stringValue());

        if (parser.getOption("-current").doubleValue() > 0) {
            System.out.println(String.format("setting minimum beam current from EPICS of %.1f nA",parser.getOption("-current").doubleValue()));
            viewer.beamMonitor = new BeamMonitor((float)parser.getOption("-current").doubleValue());
            viewer.beamMonitor.start();
        }

        // Deal with -autosave option:
        if (parser.getOption("-autosave").intValue() > 0) {
            final int n = parser.getOption("-autosave").intValue();
            System.out.println(String.format("enabling autosave every %d events",n));
            viewer.autoSave = true;
            viewer.histoResetEvents = n;
        }

        // Deal with -outDir option:
        if (parser.getOption("-outDir") != null) {
            viewer.outputDirectory = parser.getOption("-outDir").stringValue();
        }

        // Deal with -tabs option:
        String tabs = parser.getOption("-tabs").stringValue();
        if(!tabs.equals("All")) {           
            if(tabs.split(":").length>0) {
                for(String tab : viewer.monitors.keySet()) {
                    viewer.monitors.get(tab).setActive(false);
                }
                for(String tab: tabs.split(":")) {
                    if(viewer.monitors.containsKey(tab.trim())) {
                        viewer.monitors.get(tab.trim()).setActive(true);
                    }
                }
            }
        }
        else System.out.println("All monitors set to active");

        // Set CCDB variation for all registered monitors:
        for (DetectorMonitor x : viewer.monitors.values()) {
            x.setVariation(parser.getOption("-variation").stringValue());
        }
        viewer.clasDecoder.setVariation(parser.getOption("-variation").stringValue());

        // Deal with -trigger option:
        String trigger = parser.getOption("-trigger").stringValue();
        if(trigger.startsWith("0x")==true) {
            viewer.triggerMask = Long.parseLong(trigger.substring(2),16);
            System.out.println("Trigger mask set to: " + trigger);
        }
        else {
            System.err.println("-trigger must be in hex and start with 0x");
            System.exit(1);
        }

        // Deal with -logbook option:
        viewer.logbookName = parser.getOption("-logbook").stringValue();
        System.out.println("Logbook set to " + viewer.logbookName);

        // Deal with -geometry option:
        int xSize = 1600;
        int ySize = 1000;        
        String geometry = parser.getOption("-geometry").stringValue();
        if(geometry.split("x").length==2){
            xSize = Integer.parseInt(geometry.split("x")[0]);
            ySize = Integer.parseInt(geometry.split("x")[1]);    
        }
        else System.out.println("Ignoring invalid -geometry format:  "+geometry);

        // Finally, start the GUI:
        viewer.init();
        JFrame frame = new JFrame("MON12");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(viewer.mainPanel);
        frame.setJMenuBar(viewer.menuBar);
        frame.setSize(xSize, ySize);
        frame.setVisible(true);
        
        if (!parser.getInputList().isEmpty()) {
            viewer.processorPane.openAndRun(parser.getInputList().get(0));
        }
        else if (parser.getOption("-batch").intValue() != 0) {
            String h = parser.getOption("-ethost").stringValue();
            String i = parser.getOption("-etip").stringValue();
            String ses = parser.getOption("-etsession").stringValue();
            viewer.processorPane.connectAndRun(i,"11111",ses);
        }

    }

    @Override
    public void processShape(DetectorShape2D dsd) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
       
}
