package org.clas.detectors;

import java.util.Arrays;
import org.clas.viewer.DetectorMonitor;
import org.jlab.groot.data.H1F;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.utils.groups.IndexedTable;

public class FCUPmonitor extends DetectorMonitor {
    
    IndexedTable fcupConfig = null;
    private int nscaler;
    private int fcup, fcupGated, slm, slmGated, clock, clockGated;
    private int fcupOld, fcupGatedOld, slmOld, slmGatedOld, clockOld, clockGatedOld;
    // clock frequency for conversion from clock counts to time:
    private static final double CLOCKFREQ=1e6; // Hz

    public FCUPmonitor(String name) {
        super(name);
        this.setDetectorTabNames("Faraday Cup");
        this.getCcdb().init(Arrays.asList(new String[]{"/runcontrol/fcup"})); 
        this.init(false);
    }

    
    @Override
    public void createHistos() {
        // create histograms
        this.setNumberOfEvents(0);
        this.nscaler = 0;
        this.fcup=0;
        this.fcupGated=0;
        this.slm=0;
        this.slmGated=0;
        this.clock=0;
        this.clockGated=0;
        this.fcupOld=0;
        this.fcupGatedOld=0;
        this.slmOld=0;
        this.slmGatedOld=0;
        this.clockOld=0;
        this.clockGatedOld=0;
        
        H1F summary = new H1F("summary","Gated Faraday Cup",100,0.0,250);
        summary.setTitleX("Gated Faraday Cup (pC)");
        summary.setTitleY("counts");
        summary.setFillColor(2);
        DataGroup sum = new DataGroup(1,1);
        sum.addDataSet(summary, 0);
        this.setDetectorSummary(sum);
        
        H1F hi_fcup = new H1F("hi_fcup","Faraday Cup", 100,0.0,250);
        hi_fcup.setTitleX("FCup (pC)");
        hi_fcup.setTitleY("Counts");
        hi_fcup.setFillColor(6);
        H1F hi_slm = new H1F("hi_slm","SLM", 100,0.0,10000);
        hi_slm.setTitleX("SLM");
        hi_slm.setTitleY("Counts");
        hi_slm.setFillColor(3);
        H1F hi_fcg = new H1F("hi_fcg","Gated Faraday Cup", 100,0.0,250);
        hi_fcg.setTitleX("Gated FCup (pC)");
        hi_fcg.setTitleY("Counts");
        hi_fcg.setFillColor(2);
        H1F hi_lt = new H1F("hi_lt","Live Time", 100,0.0,110);
        hi_lt.setTitleX("LT(%)");
        hi_lt.setTitleY("Counts");    
        hi_lt.setFillColor(4);
        DataGroup dg = new DataGroup(2,2);
        dg.addDataSet(hi_fcup, 0);
        dg.addDataSet(hi_slm,  1);
        dg.addDataSet(hi_fcg,  2);
        dg.addDataSet(hi_lt,   3);
        this.getDataGroup().add(dg, 0,0,0);
    }
        
    @Override
    public void plotHistos() {
        // initialize canvas and plot histograms
        this.getDetectorCanvas().getCanvas("Faraday Cup").divide(2, 2);
        this.getDetectorCanvas().getCanvas("Faraday Cup").setGridX(false);
        this.getDetectorCanvas().getCanvas("Faraday Cup").setGridY(false);
        this.getDetectorCanvas().getCanvas("Faraday Cup").cd(0);
        this.getDetectorCanvas().getCanvas("Faraday Cup").draw(this.getDataGroup().getItem(0,0,0).getH1F("hi_fcup"));
        this.getDetectorCanvas().getCanvas("Faraday Cup").cd(1);
        this.getDetectorCanvas().getCanvas("Faraday Cup").draw(this.getDataGroup().getItem(0,0,0).getH1F("hi_slm"));
        this.getDetectorCanvas().getCanvas("Faraday Cup").cd(2);
        this.getDetectorCanvas().getCanvas("Faraday Cup").draw(this.getDataGroup().getItem(0,0,0).getH1F("hi_fcg"));
        this.getDetectorCanvas().getCanvas("Faraday Cup").cd(3);
        this.getDetectorCanvas().getCanvas("Faraday Cup").draw(this.getDataGroup().getItem(0,0,0).getH1F("hi_lt"));
        this.getDetectorCanvas().getCanvas("Faraday Cup").update();
        
    }

    @Override
    public void processEvent(DataEvent event) {

        DataBank scaler = null;
        if (event.hasBank("RAW::scaler")) scaler = event.getBank("RAW::scaler");

        fcupConfig = this.getCcdb().getConstants(runNumber, "/runcontrol/fcup");
        double fcup_slope  = fcupConfig.getDoubleValue("slope",0,0,0);
        double fcup_offset = fcupConfig.getDoubleValue("offset",0,0,0);
        double fcup_atten  = fcupConfig.getIntValue("atten",0,0,0);
       
        //System.out.println("fcup_slope  = " + fcup_slope );
        //System.out.println("fcup_offset = " + fcup_offset);
        //System.out.println("fcup_atten  = " + fcup_atten );

        if(scaler!=null) {
    //   config.show();
    //   scaler.show();
            //Different scaler inputs are identified by the channel number as follows:
            //channel = i + 16 * j
            //with:
            //- k = 0,1,2 -> FCUP, SLM, Clock
            //- j = 0,1,2,3 -> gated TRG, gated TDC, ungated TRG, ungated TDC
            //Gating is done with the BUSY signal of the DAQ, which implies that for example the gated clock gives the dead time.

            int[][] scalerValue = new int[3][4]; 
            nscaler += scaler.rows()-12;
            for(int i=0; i<scaler.rows(); i++) {
                int crate   = scaler.getByte("crate",i);
                int slot    = scaler.getByte("slot",i);
                int channel = scaler.getShort("channel",i);
                int value   = (int) scaler.getLong("value",i);
                if(slot==64) {
                    int j = (int) channel/16;
                    int k = channel%16;
                    scalerValue[k][j]=value;
                }
            }
            this.fcupOld       = this.fcup;
            this.fcupGatedOld  = this.fcupGated;
            this.slmOld        = this.slm;
            this.slmGatedOld   = this.slmGated;
            this.clockOld      = this.clock;
            this.clockGatedOld = this.clockGated;
            this.fcup       = scalerValue[0][2];
            this.slm        = scalerValue[1][2]; 
            this.clock      = scalerValue[2][2];
            this.fcupGated  = scalerValue[0][2]-scalerValue[0][0];
            this.slmGated   = scalerValue[1][2]-scalerValue[1][0];
            this.clockGated = scalerValue[2][2]-scalerValue[2][0];
            double fc  = ((double) (this.fcup-this.fcupOld)-fcup_offset*((double)(this.clock-this.clockOld)/CLOCKFREQ)) / fcup_slope;
            double fcg = ((double) (this.fcupGated-this.fcupGatedOld)-fcup_offset*((double)(this.clock-this.clockOld)/CLOCKFREQ)) / fcup_slope;
            this.getDataGroup().getItem(0,0,0).getH1F("hi_fcup").fill(fc*1000);
            this.getDataGroup().getItem(0,0,0).getH1F("hi_slm").fill(this.slm-this.slmOld);
            this.getDataGroup().getItem(0,0,0).getH1F("hi_fcg").fill(fcg*1000);
            this.getDataGroup().getItem(0,0,0).getH1F("hi_lt").fill(100*((double) (this.clockGated-this.clockGatedOld))/((double)(this.clock-this.clockOld)));

            this.getDetectorSummary().getH1F("summary").fill(fcg*1000);
        }
    }


    @Override
    public void analysisUpdate() {

    }
    
}
