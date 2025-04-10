package org.clas.detectors;

import org.clas.viewer.DetectorMonitor;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;

import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList; // import the ArrayList class
import java.lang.Math;

import java.util.Arrays;
import org.jlab.utils.groups.IndexedTable;

import javax.swing.JSlider;


/**
 *
 * @author
 */

public class ALERTMonitor extends DetectorMonitor {

    static final double charge_min = 20.0;
    private double fc = 0.0;

    static final int FPS_MIN = 0;
    static final int FPS_MAX = 30;
    static final int FPS_INIT = 15;    //initial frames per second
    JSlider framesPerSecond;

    static final int[] layer_stereo  = {10,-10,-10,10,10,-10,-10,10};
    static final int[] layer_wires  = {94/2,112/2,112/2,144/2,144/2,174/2,174/2,198/2};
    static final int[] layer_radius  = {30+2*1 ,30+2*4,30+2*6,30+2*9,30+2*11,30+2*14,30+2*16,30+2*19};

    //public static class HitPoint {
    //    double x  = 0.0;
    //    double y  = 0.0;
    //}

    /**returns xy
     */
    public static double[] ahdcXYPosition(int layer, int comp){
        double[] res = {0.0,0.0};
        //if(comp > layer_wires[layer-1]  ){
        //    //errror
        //    return res;
        //}
        double r = layer_radius[layer-1];
        //System.out.println("comp " + comp );
        //System.out.println("r " + r );
        //System.out.println("layer_wires[layer-1] " + layer_wires[layer-1] );

        double nwires=layer_wires[layer-1];
        double phi = 2.0*Math.PI*((comp-1.0)/nwires);
        //System.out.println("phi " + phi );
        res[0] = r*Math.cos(phi);
        res[1] = r*Math.sin(phi);
        //System.out.println("res[0] " + res[0]  );
        //System.out.println("res[1] " + res[1]  );
        return res;
    }


    /** returns non zero if all 8 layers have a hit.
     * Argument takes an array of 8 shorts
     */
    public static int allLayersHit(int [] layerhit){
        int all_layers=1;
        for(int ilay = 0; ilay < 8; ilay++){
            if( layerhit[ilay] == 0) {
                all_layers=0;
                break;
            }
        }
        return all_layers;
    }
    /** returns number of layers hit
     */
    public static int nLayersHit(int [] layerhit){
        int layers=0;
        for(int ilay = 0; ilay < 8; ilay++){
            if( layerhit[ilay] != 0) {
                layers++;
            }
        }
        return layers;
    }

    /** Helper function to map layer "bank layer number" to 
     * incremental layer number. 
     */
    public static int getLayerNumber(int bank_layer_number){ 
        int layer_number = 0;
        switch (bank_layer_number) {
            case 11 :
                layer_number = 1;
                break;
            case 21 :
                layer_number = 2;
                break;
            case 22 :
                layer_number = 3;
                break;
            case 31 :
                layer_number = 4;
                break;
            case 32 :
                layer_number = 5;
                break;
            case 41 :
                layer_number = 6;
                break;
            case 42 :
                layer_number = 7;
                break;
            case 51 :
                layer_number = 8;
                break;
        }
        return layer_number;
    }


    class HTPoint {
        double u  = 0.0;
        double v  = 0.0;
        HTPoint(double[] h, double[] ref){
            double xx = h[0]-ref[0];
            double yy = h[1]-ref[1];
            this.u = xx/(xx*xx + yy*yy) ;
            this.v = -yy/(xx*xx + yy*yy) ;
        }
        double getPhiDeg() { 
            return Math.atan(this.u/this.v)*180.0/Math.PI;
        }
    }


    class IntPair {
        final int layer;
        final int hit;
        IntPair(int l, int h) {this.layer=l;this.hit=h;}
    }


    // Temporary storage for Bar TDCs per event
    private Map<Integer, Integer> barTDCOrder0Map;
    private Map<Integer, Integer> barTDCOrder1Map;

    // key: layer , value: hit number
    private Map<Integer, List<Integer>> ahdc_layer_hit_map;

    private float tdc_bin_time = 0.015625f; // ns/bin


    IndexedTable fcupConfig = null;
    private int nscaler;
    private int fcup, fcupGated, slm, slmGated, clock, clockGated;
    private int fcupOld, fcupGatedOld, slmOld, slmGatedOld, clockOld, clockGatedOld;
    // clock frequency for conversion from clock counts to time:
    private static final double CLOCKFREQ=1e6; // Hz
    private double fcup_slope ;
    private double fcup_offset;
    private double fcup_atten     ;

    private int nevents_good_current     ;

    public ALERTMonitor(String name) {
        super(name);
        // Add new tabs: "WedgeTDC", "BarTDC", "BarSumDiff"
        this.setDetectorTabNames("Bar Sum and Diff","Hough");
        this.getCcdb().init(Arrays.asList(new String[]{"/runcontrol/fcup"})); 
        this.init(false);

        // Initialize temporary storage maps
        barTDCOrder0Map = new HashMap<>();
        barTDCOrder1Map = new HashMap<>();

        ahdc_layer_hit_map  = new HashMap<>();
        for(int i = 0; i< 8; i++) {
            ahdc_layer_hit_map.put(i+1,new ArrayList<>());
        }
        framesPerSecond = new JSlider(JSlider.HORIZONTAL,
                FPS_MIN, FPS_MAX, FPS_INIT);

        this.nevents_good_current =0;

    }

    @Override
    public void createHistos() {
        // Initialize canvas and create histograms
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
        this.fc = 0.0;

        fcupConfig = this.getCcdb().getConstants(runNumber, "/runcontrol/fcup");
        fcup_slope  = fcupConfig.getDoubleValue("slope",0,0,0);
        fcup_offset = fcupConfig.getDoubleValue("offset",0,0,0);
        fcup_atten  = fcupConfig.getIntValue("atten",0,0,0);
        System.out.println("fcup_slope  = " + fcup_slope );
        System.out.println("fcup_offset = " + fcup_offset);
        System.out.println("fcup_atten  = " + fcup_atten );


        //// Module Canvas
        this.getDetectorCanvas().getCanvas("Hough").divide(2, 2);
        this.getDetectorCanvas().getCanvas("Hough").setGridX(false);
        this.getDetectorCanvas().getCanvas("Hough").setGridY(false);


        String run_number_stub = " [run:" + runNumber + "]";

        H1F hough1 = new H1F("hough1", "hough1 "+run_number_stub, 30, -90, 90);
        hough1.setTitleX("phi");
        hough1.setTitleY("hits");
        hough1.setFillColor(36);

        H1F hough2 = new H1F("hough2", "hough2 "+run_number_stub, 30, -90, 90);
        hough2.setTitleX("phi");
        hough2.setTitleY("hits");
        hough2.setFillColor(36);


        H2F hough_uv1 = new H2F("hough_uv1", "hough_uv1 "+run_number_stub, 100, -0.5, 0.5, 100, -0.5, 0.5);
        hough_uv1.setTitleX("u");
        hough_uv1.setTitleY("v");

        H2F hough_uv2 = new H2F("hough_uv2", "hough_uv2 "+run_number_stub, 100, -0.5, 0.5, 100, -0.5, 0.5);
        hough_uv2.setTitleX("u");
        hough_uv2.setTitleY("v");

        // add graph to DataGroup
        DataGroup htg = new DataGroup(); 
        htg.addDataSet(hough1, 0);
        htg.addDataSet(hough2, 1);
        htg.addDataSet(hough_uv1, 2);
        htg.addDataSet(hough_uv2, 3);
        this.getDataGroup().add(htg,2,0,0);

        //// WedgeTDC Canvas (3x5 grid for sectors 0-14)
        //this.getDetectorCanvas().getCanvas("Wedge TDCs").divide(3, 5);
        //this.getDetectorCanvas().getCanvas("Wedge TDCs").setGridX(false);
        //this.getDetectorCanvas().getCanvas("Wedge TDCs").setGridY(false);

        //// BarTDC Canvas (3x5 grid for sectors 0-14)
        //this.getDetectorCanvas().getCanvas("Bar TDCs").divide(3, 5);
        //this.getDetectorCanvas().getCanvas("Bar TDCs").setGridX(false);
        //this.getDetectorCanvas().getCanvas("Bar TDCs").setGridY(false);

        // BarSumDiff Canvas
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").divide(4, 1);
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").setGridX(false);
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").setGridY(false);


        // BarSumDiff Histograms
        int barsum_peak = 24000; // peak location used to center the histogram binning:
        H1F barSum = new H1F("barSum", "Bar TDC Sum", 100, barsum_peak-20000, barsum_peak+20000); // Assuming sum range
        barSum.setTitleX("Sum of TDCs (Order0 + Order1)");
        barSum.setTitleY("Counts");
        barSum.setFillColor(46);
        H1F barSum2 = new H1F("barSum2", "Bar TDC Sum per 1k events", 100, barsum_peak-20000, barsum_peak+20000); // Assuming sum range
        barSum2.setTitleX("Sum of TDCs (Order0 + Order1)");
        barSum2.setTitleY("Counts");
        barSum2.setFillColor(46);
        // Uncomment if you have a method to disable statistics box
        // barSum.setOptStat(0);
        float barsumtime_peak = 36000*tdc_bin_time; // peak location used to center the histogram binning:
        H1F barSumTime = new H1F("barSumTime", "Bar Sum Time", 200, 100,700);//barsum_peak*tdc_bin_time-20000*tdc_bin_time, barsum_peak*tdc_bin_time+20000*tdc_bin_time); // Assuming sum range
        barSumTime.setTitleX("Sum of Times (Order0 + Order1)");
        barSumTime.setTitleY("Counts");
        barSumTime.setFillColor(46);

        H1F barDiff = new H1F("barDiff", "Bar TDC Difference", 100, -20000, 20000); // Assuming difference range
        barDiff.setTitleX("Difference of TDCs (Order1 - Order0)");
        barDiff.setTitleY("Counts");
        barDiff.setFillColor(38);
        H1F barDiff2 = new H1F("barDiff2", "Bar TDC Diff per 1k events", 100, -20000, 20000); // Assuming difference range
        barDiff2.setTitleX("Difference of TDCs (Order1 - Order0)");
        barDiff2.setTitleY("Counts/1k events");
        barDiff2.setFillColor(38);

        H1F barDiffTime = new H1F("barDiffTime", "Bar TDC Difference", 200, -10, 10);
        barDiffTime.setTitleX("Difference of Times (Order1 - Order0)");
        barDiffTime.setTitleY("Counts");
        barDiffTime.setFillColor(38);
        // Uncomment if you have a method to disable statistics box
        // barDiff.setOptStat(0);
        //

        //// Create data group for existing and new histograms
        //DataGroup dg = new DataGroup();
        //dg.addDataSet(rawTDC, 0);
        //dg.addDataSet(occTDC, 0);
        //dg.addDataSet(occTDC1D, 0);
        //dg.addDataSet(tdc, 0);
        //dg.addDataSet(wedgeScalers, 0);
        //dg.addDataSet(barScalers, 0);
        //this.getDataGroup().add(dg, 1, 0, 0); // tab group 1

        //// Create data groups for new histograms
        //this.getDataGroup().add(wedgeTDCGroup, 4, 0, 0); // Tab index 4: "WedgeTDC"
        //this.getDataGroup().add(barTDCGroup, 5, 0, 0);   // Tab index 5: "BarTDC"
        //this.getDataGroup().add(wedgeToTGroup, 4, 1, 0); 
        //this.getDataGroup().add(barToTGroup, 5, 1, 0);   

        DataGroup barSumDiffGroup = new DataGroup();
        barSumDiffGroup.addDataSet(barSum, 0);
        barSumDiffGroup.addDataSet(barDiff, 1);
        barSumDiffGroup.addDataSet(barSumTime, 2);
        barSumDiffGroup.addDataSet(barDiffTime, 3);
        barSumDiffGroup.addDataSet(barDiff2, 4);
        barSumDiffGroup.addDataSet(barSum2, 5);
        this.getDataGroup().add(barSumDiffGroup, 6, 0, 0); // Tab index 6: "BarSumDiff"

    }

    @Override
    public void plotHistos() {

        // Plotting existing histograms
        this.getDetectorCanvas().getCanvas("Hough").cd(0);
        this.getDetectorCanvas().getCanvas("Hough").draw(this.getDataGroup().getItem(2, 0, 0).getH1F("hough1"));
        this.getDetectorCanvas().getCanvas("Hough").cd(1);
        this.getDetectorCanvas().getCanvas("Hough").draw(this.getDataGroup().getItem(2, 0, 0).getH1F("hough2"));
        this.getDetectorCanvas().getCanvas("Hough").cd(2);
        this.getDetectorCanvas().getCanvas("Hough").draw(this.getDataGroup().getItem(2, 0, 0).getH2F("hough_uv1"));
        this.getDetectorCanvas().getCanvas("Hough").cd(3);
        this.getDetectorCanvas().getCanvas("Hough").draw(this.getDataGroup().getItem(2, 0, 0).getH2F("hough_uv2"));

        //// Plot BarTDC Histograms
        //DataGroup barTDCGroup = this.getDataGroup().getItem(5, 0, 0);
        //for (int sector = 0; sector < 15; sector++) {
        //  this.getDetectorCanvas().getCanvas("Bar TDCs").cd(sector);
        //  this.getDetectorCanvas().getCanvas("Bar TDCs").getPad(sector).setPalette("kCool");
        //  H2F barTDC = barTDCGroup.getH2F("barTDC_sector_" + sector);
        //  this.getDetectorCanvas().getCanvas("Bar TDCs").draw(barTDC);
        //  //this.getDetectorCanvas().getCanvas("Bar TDCs").getPad(sector).getAxisY().setLog(true);
        //}
        //this.getDetectorCanvas().getCanvas("Bar TDCs").update();

        // Plot BarSumDiff Histograms
        DataGroup barSumDiffGroup = this.getDataGroup().getItem(6, 0, 0);
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").cd(0);
        H1F barSum = barSumDiffGroup.getH1F("barSum2");
        barSum.setFillColor(46);
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").draw(barSum);
        //this.getDetectorCanvas().getCanvas("Bar Sum and Diff").getPad(0).getAxisY().setLog(true);

        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").cd(1);
        H1F barDiff2 = barSumDiffGroup.getH1F("barDiff2");
        barDiff2.setFillColor(38);
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").draw(barDiff2);
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").update();
        //this.getDetectorCanvas().getCanvas("Bar Sum and Diff").getPad(1).getAxisY().setLog(true);


        // Plot BarSumDiff Histograms
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").cd(2);
        H1F barSumTime = barSumDiffGroup.getH1F("barSumTime");
        barSumTime.setFillColor(46);
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").draw(barSumTime);
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").getPad(2).getAxisY().setLog(true);


        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").cd(3);
        H1F barDiffTime = barSumDiffGroup.getH1F("barDiffTime");
        barDiffTime.setFillColor(38);
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").draw(barDiffTime);
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").update();
        this.getDetectorCanvas().getCanvas("Bar Sum and Diff").getPad(3).getAxisY().setLog(true);


        //// Plot WedgeToT Histograms
        //DataGroup wedgeToTGroup = this.getDataGroup().getItem(4, 1, 0);
        //for (int sector = 0; sector < 15; sector++) {
        //  this.getDetectorCanvas().getCanvas("Wedge ToTs").cd(sector);
        //  this.getDetectorCanvas().getCanvas("Wedge ToTs").getPad(sector).setPalette("kCool");
        //  H2F wedgeToT = wedgeToTGroup.getH2F("wedgeToT_sector_" + sector);
        //  this.getDetectorCanvas().getCanvas("Wedge ToTs").draw(wedgeToT);
        //  //this.getDetectorCanvas().getCanvas("Wedge ToTs").getPad(sector).getAxisY().setLog(true);
        //}
        //this.getDetectorCanvas().getCanvas("Wedge ToTs").update();

        //// Plot BarToT Histograms
        //DataGroup barToTGroup = this.getDataGroup().getItem(5, 1, 0);
        //for (int sector = 0; sector < 15; sector++) {
        //  this.getDetectorCanvas().getCanvas("Bar ToTs").cd(sector);
        //  this.getDetectorCanvas().getCanvas("Bar ToTs").getPad(sector).setPalette("kCool");
        //  H2F barToT = barToTGroup.getH2F("barToT_sector_" + sector);
        //  this.getDetectorCanvas().getCanvas("Bar ToTs").draw(barToT);
        //  //this.getDetectorCanvas().getCanvas("Bar ToTs").getPad(sector).getAxisY().setLog(true);
        //}
        //this.getDetectorCanvas().getCanvas("Bar ToTs").update();


        // Update detector view
        this.getDetectorView().getView().repaint();
        this.getDetectorView().update();
    }

    @Override
    public void processEvent(DataEvent event) {

        // Clear temporary storage at the start of each event
        barTDCOrder0Map.clear();
        barTDCOrder1Map.clear();
        for(int i = 0; i< 8; i++) {
            //System.out.println("layer map " + i + " size: " + ahdc_layer_hit_map.get(i+1).size());
            ahdc_layer_hit_map.get(i+1).clear();
        }
        H1F ht1 = this.getDataGroup().getItem(2, 0, 0).getH1F("hough1");
        H1F ht2 = this.getDataGroup().getItem(2, 0, 0).getH1F("hough2");
        H2F htuv1 = this.getDataGroup().getItem(2, 0, 0).getH2F("hough_uv1");
        H2F htuv2 = this.getDataGroup().getItem(2, 0, 0).getH2F("hough_uv2");
        //ht1.reset();
        //ht2.reset();

        // Get scalers  for cutting on beam current (not run time epics value.
        // note this is not perfect as it is really applies to previous events
        //DataBank scaler = null;
        //if (event.hasBank("RAW::scaler")) {
        //    scaler = event.getBank("RAW::scaler");

        //    double fc = 0.0;

        //    if(scaler!=null) {
        //        //   config.show();
        //        //   scaler.show();
        //        //Different scaler inputs are identified by the channel number as follows:
        //        //channel = i + 16 * j
        //        //with:
        //        //- k = 0,1,2 -> FCUP, SLM, Clock
        //        //- j = 0,1,2,3 -> gated TRG, gated TDC, ungated TRG, ungated TDC
        //        //Gating is done with the BUSY signal of the DAQ, which implies that for example the gated clock gives the dead time.

        //        int[][] scalerValue = new int[3][4]; 
        //        nscaler += scaler.rows()-12;
        //        for(int i=0; i<scaler.rows(); i++) {
        //            int crate   = scaler.getByte("crate",i);
        //            int slot    = scaler.getByte("slot",i);
        //            int channel = scaler.getShort("channel",i);
        //            int value   = (int) scaler.getLong("value",i);
        //            if(slot==64) {
        //                int j = (int) channel/16;
        //                int k = channel%16;
        //                scalerValue[k][j]=value;
        //            }
        //        }
        //        this.fcupOld       = this.fcup;
        //        this.fcupGatedOld  = this.fcupGated;
        //        this.slmOld        = this.slm;
        //        this.slmGatedOld   = this.slmGated;
        //        this.clockOld      = this.clock;
        //        this.clockGatedOld = this.clockGated;
        //        this.fcup       = scalerValue[0][2];
        //        this.slm        = scalerValue[1][2]; 
        //        this.clock      = scalerValue[2][2];
        //        this.fcupGated  = scalerValue[0][2]-scalerValue[0][0];
        //        this.slmGated   = scalerValue[1][2]-scalerValue[1][0];
        //        this.clockGated = scalerValue[2][2]-scalerValue[2][0];
        //        fc  = 1000.0*((double) (this.fcup-this.fcupOld)-fcup_offset*((double)(this.clock-this.clockOld)/CLOCKFREQ)) / fcup_slope;
        //        double fcg = ((double) (this.fcupGated-this.fcupGatedOld)-fcup_offset*((double)(this.clock-this.clockOld)/CLOCKFREQ)) / fcup_slope;
        //    }

        //    //String run_number_stub = " [run:" + runNumber + ", ev:" + event_number+ "]";

        //    //if(fc < charge_min)  return;
        //    System.out.println("fc = " + fc);
        //}


        // Run info
        int event_number = 0;
        if(event.hasBank("RUN::config")==true){
            DataBank bank = event.getBank("RUN::config");
            event_number = bank.getInt("event", 0);
        }
        //String run_number_stub = " [run:" + runNumber + ", ev:" + event_number+ "]";
        //ht1.setTitle("hough1"+ run_number_stub);
        //ht2.setTitle("hough1"+ run_number_stub);




        if(event.hasBank("AHDC::adc")==true){
            //System.out.println(" has AHDC bank!");
            DataBank bank = event.getBank("AHDC::adc");
            int rows = bank.rows();

            for(int loop = 0; loop < rows; loop++){
                int sector  = bank.getByte("sector", loop);
                int layer   = bank.getByte("layer", loop);
                int comp    = bank.getShort("component", loop);
                int order   = bank.getByte("order", loop);
                int adc     = bank.getInt("ADC", loop);
                float time  = bank.getFloat("time", loop);
                float leadingEdgeTime = bank.getFloat("leadingEdgeTime", loop);
                float timeOverThreshold = bank.getFloat("timeOverThreshold", loop);
                float constantFractionTime = bank.getFloat("constantFractionTime", loop);
                int integral = bank.getInt("integral", loop);

                if(adc>=50 && time>=0) {
                    int layer_number = ALERTMonitor.getLayerNumber(layer);
                //System.out.println("layer " + layer_number);
                    ahdc_layer_hit_map.get(layer_number).add(loop);
                }
            }

            // hough transform
            // positive layers
            for(int ih = 0 ; ih< ahdc_layer_hit_map.get(1).size(); ih++) {
                int hit =  ahdc_layer_hit_map.get(1).get(ih);
                int layer   = bank.getByte("layer", hit);
                int comp    = bank.getShort("component", hit);
                int layer_number = ALERTMonitor.getLayerNumber(layer);
                double[] ref = ahdcXYPosition(layer_number,comp);
                //System.out.println("comp " + comp );

                // loop over layers 2-8 and compute hough transforms for the refernce
                for(int il = 2 ; il<= 8; il++) {
                    //skip negative stereo angles
                    if(layer_stereo[il-1]<0) continue; 

                    int layer_nhits = ahdc_layer_hit_map.get(il).size();

                    for(int ih2 = 0 ; ih2<layer_nhits; ih2++) {
                        int hit2        = ahdc_layer_hit_map.get(il).get(ih2);
                        //System.out.println("layer " + il+" hit2: " + hit2);
                        int layer2      = bank.getByte("layer", hit2);
                        int comp2       = bank.getShort("component", hit2);
                        int layer_number2 = ALERTMonitor.getLayerNumber(layer2);
                        double[] xy_hit = ahdcXYPosition(layer_number2,comp2);
                        HTPoint   p     = new HTPoint(xy_hit,ref);
                        ht1.fill(p.getPhiDeg());
                        htuv1.fill(p.u,p.v);


                    }
                }
            }
            //negative layers
            for(int ih = 0 ; ih< ahdc_layer_hit_map.get(2).size(); ih++) {
                int hit =  ahdc_layer_hit_map.get(2).get(ih);
                int layer   = bank.getByte("layer", hit);
                int comp    = bank.getShort("component", hit);
                int layer_number = ALERTMonitor.getLayerNumber(layer);
                double[] ref = ahdcXYPosition(layer_number,comp);
                //System.out.println("comp " + comp );
                for(int il = 3 ; il<= 8; il++) {
                    //skip negative stereo angles
                    if(layer_stereo[il-1]>0) continue; 

                    int layer_nhits = ahdc_layer_hit_map.get(il).size();

                    for(int ih2 = 0 ; ih2<layer_nhits; ih2++) {
                        int hit2        = ahdc_layer_hit_map.get(il).get(ih2);
                        //System.out.println("layer " + il+" hit2: " + hit2);
                        int layer2      = bank.getByte("layer", hit2);
                        int comp2       = bank.getShort("component", hit2);
                        int layer_number2 = ALERTMonitor.getLayerNumber(layer2);
                        double[] xy_hit = ahdcXYPosition(layer_number2,comp2);
                        HTPoint   p     = new HTPoint(xy_hit,ref);
                        ht2.fill(p.getPhiDeg());
                        htuv2.fill(p.u,p.v);
                    }
                }
            }
        }

        // Process event info and save into data group
        if (event.hasBank("ATOF::tdc")) {
            DataBank bank = event.getBank("ATOF::tdc");
            int rows = bank.rows();
            for (int loop = 0; loop < rows; loop++) {

                int sector = bank.getByte("sector", loop);
                int layer = bank.getByte("layer", loop);
                int comp = bank.getShort("component", loop);
                int order = bank.getByte("order", loop);
                int tdc = bank.getInt("TDC", loop);
                int tot = bank.getInt("ToT", loop);
                int w = sector*4 + layer ;

                //if (tot > 1000) {
                //  System.out.println("ROW " + loop + " SECTOR = " + sector + " LAYER = " + layer + " COMPONENT = " + comp + " ORDER = " + order +
                //      " TDC = " + tdc + " ToT = " + tot);
                //}
                if (tot > 0) {
                    int wire = (layer - 1) * 100 + comp;
                    //this.getDataGroup().getItem(1, 0, 0).getH2F("occTDC").fill(comp, layer);
                    //this.getDataGroup().getItem(1, 0, 0).getH1F("occTDC1D").fill(wire);
                    //this.getDataGroup().getItem(1, 0, 0).getH2F("tdc").fill(tdc * 1.0, wire);
                    //this.getDetectorSummary().getH1F("summary").fill(sector);

                    //if(comp <10) {
                    //  this.getDataGroup().getItem(0, 0, 0).getH1F("module_wedge_hits").fill(sector);

                    //  this.getDataGroup().getItem(2, 0, 0).getH1F("globalwedge_wedge_hits").fill(w);
                    //  this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_wedge_vs_z_hits").fill(w,comp);

                    //} else{
                    //  this.getDataGroup().getItem(0, 0, 0).getH1F("module_bar_hits").fill(sector);

                    //  this.getDataGroup().getItem(2, 0, 0).getH1F("globalwedge_bar_hits").fill(w);
                    //}

                    //this.getDataGroup().getItem(0, 0, 0).getH2F("module_vs_z_wedge_hits").fill(sector,comp+order);
                    //this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_all_vs_z_hits").fill(w,comp+order);
                    ////this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_all_vs_z_occ").fill(w,comp+order);



                    int xbin = sector * 4 + layer ;
                    // Fill Wedge Scalers histogram
                    if (comp >= 0 && comp <= 9) {
                        ////this.getDataGroup().getItem(1, 0, 0).getH2F("wedgeScalers").fill(xbin, comp);
                        //// Fill Wedge TDC histogram
                        //DataGroup wedgeTDCGroup = this.getDataGroup().getItem(4, 0, 0);
                        //DataGroup wedgeToTGroup = this.getDataGroup().getItem(4, 1, 0);
                        //if (sector < 15) { // Ensure sector is within 0-14
                        //  H2F wedgeTDC = wedgeTDCGroup.getH2F("wedgeTDC_sector_" + sector);
                        //  wedgeTDC.fill(tdc * tdc_bin_time, layer*10 + comp);
                        //  H2F wedgeToT = wedgeToTGroup.getH2F("wedgeToT_sector_" + sector);
                        //  wedgeToT.fill(tot * tdc_bin_time, layer*10 + comp);
                        //}
                    }
                    // Fill Bar Scalers histogram
                    else if (comp == 10) {
                        //this.getDataGroup().getItem(1, 0, 0).getH2F("barScalers").fill(xbin, order);
                        //// Fill Bar TDC histogram
                        //DataGroup barTDCGroup = this.getDataGroup().getItem(5, 0, 0);
                        //DataGroup barToTGroup = this.getDataGroup().getItem(5, 1, 0);
                        //if (sector < 15) { // Ensure sector is within 0-14
                        //  H2F barTDC = barTDCGroup.getH2F("barTDC_sector_" + sector);
                        //  barTDC.fill(tdc * tdc_bin_time,order*4+layer);
                        //  H2F barToT = barToTGroup.getH2F("barToT_sector_" + sector);
                        //  barToT.fill(tot * tdc_bin_time,order*4+layer);
                        //}

                        // Store TDCs based on order
                        if (order == 0) {
                            barTDCOrder0Map.put(xbin, tdc);
                        } else if (order == 1) {
                            barTDCOrder1Map.put(xbin, tdc);
                        }
                    }
                }
            }

            // After processing all hits, compute sum and difference for bars where both orders are present
            DataGroup barSumDiffGroup = this.getDataGroup().getItem(6, 0, 0);
            H1F barSum = barSumDiffGroup.getH1F("barSum");
            H1F barDiff = barSumDiffGroup.getH1F("barDiff");
            H1F barSumTime = barSumDiffGroup.getH1F("barSumTime");
            H1F barDiffTime = barSumDiffGroup.getH1F("barDiffTime");

            for (Integer xbin : barTDCOrder0Map.keySet()) {
                if (barTDCOrder1Map.containsKey(xbin)) {
                    int tdc0 = barTDCOrder0Map.get(xbin);
                    int tdc1 = barTDCOrder1Map.get(xbin);
                    int sum = tdc0 + tdc1;
                    int diff = tdc1 - tdc0;
                    barSum.fill(sum * 1.0);
                    barDiff.fill(diff * 1.0);
                    barSumTime.fill(sum * tdc_bin_time);
                    barDiffTime.fill(diff * tdc_bin_time);
                    //System.out.println("sum = " + sum + ", diff = " +  diff);
                }
            }
        }

        this.nevents_good_current++;
    }

    @Override
    public void analysisUpdate() {
        if (this.nevents_good_current > 0) {

            DataGroup barSumDiffGroup = this.getDataGroup().getItem(6, 0, 0);
            H1F barSum = barSumDiffGroup.getH1F("barSum");
            H1F barSum2 = barSumDiffGroup.getH1F("barSum2");
            H1F barDiff = barSumDiffGroup.getH1F("barDiff");
            H1F barDiff2 = barSumDiffGroup.getH1F("barDiff2");
            H1F barSumTime = barSumDiffGroup.getH1F("barSumTime");
            H1F barDiffTime = barSumDiffGroup.getH1F("barDiffTime");
            double norm = 1.0e3/this.nevents_good_current;

            for (int loop = 0; loop < barDiff.getDataSize(0); loop++) {
                barDiff2.setBinContent(loop, barDiff.getBinContent(loop) *norm);
            }
            for (int loop = 0; loop < barSum.getDataSize(0); loop++) {
                barSum2.setBinContent(loop, barSum.getBinContent(loop) *norm);
            }

            //H2F hits = this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_all_vs_z_hits");
            //for (int loop = 0; loop < hits.getDataBufferSize(); loop++) {
            //  this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_all_vs_z_occ").setDataBufferBin(loop, 100.0*hits.getDataBufferBin(loop) / this.getNumberOfEvents());
            //}
            //H2F raw = this.getDataGroup().getItem(1, 0, 0).getH2F("rawTDC");
            //for (int loop = 0; loop < raw.getDataBufferSize(); loop++) {
            //  this.getDataGroup().getItem(1, 0, 0).getH2F("occTDC").setDataBufferBin(loop, 100 * raw.getDataBufferBin(loop) / this.getNumberOfEvents());
            //}
        }
    }
}
