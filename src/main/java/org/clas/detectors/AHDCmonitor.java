package org.clas.detectors;


import org.clas.viewer.DetectorMonitor;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;

/** ALERT HDC.
 *
 * @author devita, whit, felix
 *
 */
public class AHDCmonitor  extends DetectorMonitor {

    static final int[] layer_wires  = {94/2,112/2,112/2,144/2,144/2,174/2,174/2,198/2};
    static final int[] layer_radius  = {30+2*1 ,30+2*4,30+2*6,30+2*9,30+2*11,30+2*14,30+2*16,30+2*19};

    public AHDCmonitor(String name) {
        super(name);
        this.setDetectorTabNames("charge", "time", "time1D", "Occupancy","1D");
        this.init(false);
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


    @Override
    public void createHistos() {
        // initialize canvas and create histograms
        this.setNumberOfEvents(0);

        this.getDetectorCanvas().getCanvas("charge").divide(1, 4);
        this.getDetectorCanvas().getCanvas("charge").setGridX(false);
        this.getDetectorCanvas().getCanvas("charge").setGridY(false);

        this.getDetectorCanvas().getCanvas("time").divide(1, 4);
        this.getDetectorCanvas().getCanvas("time").setGridX(false);
        this.getDetectorCanvas().getCanvas("time").setGridY(false);

        this.getDetectorCanvas().getCanvas("time1D").divide(2, 3);
        this.getDetectorCanvas().getCanvas("time1D").setGridX(false);
        this.getDetectorCanvas().getCanvas("time1D").setGridY(false);

        this.getDetectorCanvas().getCanvas("Occupancy").divide(2, 3);
        this.getDetectorCanvas().getCanvas("Occupancy").setGridX(false);
        this.getDetectorCanvas().getCanvas("Occupancy").setGridY(false);

        this.getDetectorCanvas().getCanvas("1D").divide(2, 3);
        this.getDetectorCanvas().getCanvas("1D").setGridX(false);
        this.getDetectorCanvas().getCanvas("1D").setGridY(false);

        // summary
        H2F summary = new H2F("summary","summary", 100, 1, 100, 8, 1, 9);
        summary.setTitleX("wire number");
        summary.setTitleY("layer number");
        summary.setTitle("AHDC (occupancy)");
        //summary.setFillColor(36);
        DataGroup sum = new DataGroup(1,1);
        sum.addDataSet(summary, 0);
        this.setDetectorSummary(sum);

        // charge
        H2F hist2d_occupancy = new H2F("occupancy", "occupancy", 100, 1, 100, 8, 1, 9);
        H2F hist2d_raw_occupancy = new H2F("raw_occupancy", "raw_occupancy", 100, 1, 100, 8, 1, 9);
        hist2d_occupancy.setTitleY("layer number");
        hist2d_occupancy.setTitleX("wire number");
        hist2d_occupancy.setTitle("occupancy [%]");

        H2F hist2d_adcMax = new H2F("adcMax", "adcMax", 100, 1, 100, 8, 1, 9);
        H2F hist2d_raw_adcMax = new H2F("raw_adcMax", "raw_adcMax", 100, 1, 100, 8, 1, 9);
        hist2d_adcMax.setTitleY("layer number");
        hist2d_adcMax.setTitleX("wire number");
        hist2d_adcMax.setTitle("< adcMax >");

        H2F hist2d_integral = new H2F("integral", "integral", 100, 1, 100, 8, 1, 9);
        H2F hist2d_raw_integral = new H2F("raw_integral", "raw_integral", 100, 1, 100, 8, 1, 9);
        hist2d_integral.setTitleY("layer number");
        hist2d_integral.setTitleX("wire number");
        hist2d_integral.setTitle("< integral >");
		// put here because it is related to the charge
        H2F hist2d_timeOverThreshold = new H2F("timeOverThreshold", "timeOverThreshold", 100, 1, 100, 8, 1, 9);
        H2F hist2d_raw_timeOverThreshold = new H2F("raw_timeOverThreshold", "raw_timeOverThreshold", 100, 1, 100, 8, 1, 9);
        hist2d_timeOverThreshold.setTitleY("layer number");
        hist2d_timeOverThreshold.setTitleX("wire number");
        hist2d_timeOverThreshold.setTitle("< timeOverThreshold >");
        // time
        H2F hist2d_timeMax = new H2F("timeMax", "timeMax", 100, 1, 100, 8, 1, 9);
        H2F hist2d_raw_timeMax = new H2F("raw_timeMax", "raw_timeMax", 100, 1, 100, 8, 1, 9);
        hist2d_timeMax.setTitleY("layer number");
        hist2d_timeMax.setTitleX("wire number");
        hist2d_timeMax.setTitle("< timeMax >");

        H2F hist2d_leadingEdgeTime = new H2F("leadingEdgeTime", "leadingEdgeTime", 100, 1, 100, 8, 1, 9);
        H2F hist2d_raw_leadingEdgeTime = new H2F("raw_leadingEdgeTime", "raw_leadingEdgeTime", 100, 1, 100, 8, 1, 9);
        hist2d_leadingEdgeTime.setTitleY("layer number");
        hist2d_leadingEdgeTime.setTitleX("wire number");
        hist2d_leadingEdgeTime.setTitle("< leadingEdgeTime >");

        H2F hist2d_constantFractionTime = new H2F("constantFractionTime", "constantFractionTime", 100, 1, 100, 8, 1, 9);
        H2F hist2d_raw_constantFractionTime = new H2F("raw_constantFractionTime", "raw_constantFractionTime", 100, 1, 100, 8, 1, 9);
        hist2d_constantFractionTime.setTitleY("layer number");
        hist2d_constantFractionTime.setTitleX("wire number");
        hist2d_constantFractionTime.setTitle("< constantFractionTime >");

        H2F hist2d_wftime = new H2F("wftime2d", "wftime2d", 100, 1, 100, 8, 1, 9);
        H2F hist2d_raw_wftime = new H2F("raw_wftime2d", "raw_wftime2d", 100, 1, 100, 8, 1, 9);
        hist2d_wftime.setTitleY("layer number");
        hist2d_wftime.setTitleX("wire number");
        hist2d_wftime.setTitle("< wftime >");

        H1F hist1d_leadingEdgeTime = new H1F("leadingEdgeTime1D", "leadingEdgeTime1D", 100, 0, 2400);
        hist1d_leadingEdgeTime.setTitleX("leadingEdgeTime (ns)");
        hist1d_leadingEdgeTime.setTitleY("count");

        H1F hist1d_timeOverThreshold = new H1F("timeOverThreshold1D", "timeOverThreshold1D", 100, 0, 2400);
        hist1d_timeOverThreshold.setTitleX("timeOverThreshold (ns)");
        hist1d_timeOverThreshold.setTitleY("count");

        H1F hist1d_constantFractionTime = new H1F("constantFractionTime1D", "constantFractionTime1D", 100, 0, 2400);
        hist1d_constantFractionTime.setTitleX("constantFractionTime (ns)");
        hist1d_constantFractionTime.setTitleY("count");

        H1F hist1d_wftime = new H1F("wftime1D", "wftime1D", 50, 0, 50);
        hist1d_wftime.setTitleX("wftime (bin)");
        hist1d_wftime.setTitleY("count");
        
        H1F hist1d_adcMax = new H1F("adcMax1D", "adcMax1D", 100, 0, 4095);
        hist1d_adcMax.setTitleX("adcMax");
        hist1d_adcMax.setTitleY("count");
	
        H1F hist1d_integral = new H1F("integral1D", "integral1D", 100, 0, 40000);
        hist1d_integral.setTitleX("integral");
        hist1d_integral.setTitleY("count");
	
	// Occupancy
        H1F hits_vs_layer = new H1F("hits_vs_layer", "raw_occupancy", 8, 1, 9);
        hits_vs_layer.setTitleY("hits");
        hits_vs_layer.setTitleX("layer");
        hits_vs_layer.setTitle("hits");

        H1F occupancy_vs_layer = new H1F("occupancy_vs_layer", "wire occupancy", 8, 1, 9);
        occupancy_vs_layer.setTitleY("wire occupancy");
        occupancy_vs_layer.setTitleX("layer");

        H2F hist2d_8layer_hits = new H2F("hist2d_8layer_hits", "hits for events with all 8 layers firing", 100, 1, 100, 8, 1, 9);
        hist2d_8layer_hits.setTitleY("layer number");
        hist2d_8layer_hits.setTitleX("wire number");

        H2F hist2d_8layer_occ = new H2F("hist2d_8layer_occ", "occupancy for events with all 8 layers firing", 100, 1, 100, 8, 1, 9);
        hist2d_8layer_hits.setTitleY("layer number");
        hist2d_8layer_hits.setTitleX("wire number");

        H2F hist2d_7layer_hits = new H2F("hist2d_7layer_hits", "hits for events with 7-8/8 layers firing", 100, 1, 100, 8, 1, 9);
        hist2d_7layer_hits.setTitleY("layer number");
        hist2d_7layer_hits.setTitleX("wire number");

        H2F hist2d_6layer_hits = new H2F("hist2d_6layer_hits", "hits for events with 4-6/8 layers firing", 100, 1, 100, 8, 1, 9);
        hist2d_6layer_hits.setTitleY("layer number");
        hist2d_6layer_hits.setTitleX("wire number");

        H1F missing_layer_hit = new H1F("missing_layer_hit", "missing layers with 7-8/8 layers firing", 8, 1, 9);
        missing_layer_hit.setTitleY("hits");
        missing_layer_hit.setTitleX("layer");

        H1F missing_layer_hit6 = new H1F("missing_layer_hit6", "missing layers with 6/8 layers firing", 8, 1, 9);
        missing_layer_hit6.setLineColor(2);
        missing_layer_hit6.setTitleY("hits");
        missing_layer_hit6.setTitleX("layer");

        H1F missing_layer_hit5 = new H1F("missing_layer_hit5", "missing layers with 4-6/8 layers firing", 8, 1, 9);
        missing_layer_hit5.setLineColor(4);
        missing_layer_hit5.setTitleY("hits");
        missing_layer_hit5.setTitleX("layer");

        H1F number_of_layers_hit = new H1F("number_of_layers_hit", "number of layers hit", 9, 0, 9);
        number_of_layers_hit.setTitleY("number of events");
        number_of_layers_hit.setTitleX("N layers firing");


        H1F tdc_values = new H1F("tdc_values", "TDC Values of layers hit", 200, 0, 400);
        tdc_values.setTitleX("TDC");

        H1F time_values = new H1F("time_values", "time Values of layers hit", 200, 0, 400);
        time_values.setTitleX("time");

        H1F average_waveform = new H1F("average_waveform", "average waveform", 50, 0, 51);
        average_waveform.setTitleX("sample");

        H1F average_waveform_raw = new H1F("average_waveform_raw", "average waveform", 50, 0, 51);
        average_waveform_raw.setTitleX("sample");

        H1F average_waveform_sample_count = new H1F("average_waveform_sample_count", "sample count", 50, 0, 51);
        average_waveform_sample_count.setTitleX("sample");

        H1F average_waveform_samples = new H1F("average_waveform_samples", "N samples", 50, 0, 51);
        average_waveform_samples.setTitleX("sample counts");



        H1F waveform_timestamp = new H1F("waveform_timestamp", "waveform timestamp", 100, 0, 1000);
        waveform_timestamp.setTitleX("wf timestamp");

        H1F waveform_time = new H1F("waveform_time", "waveform time", 100, 0, 1000);
        waveform_time.setTitleX("wf time");


        // add graph to DataGroup
        DataGroup dg = new DataGroup(14,1); 
        dg.addDataSet(hist2d_occupancy, 0);
        dg.addDataSet(hist2d_adcMax, 1);
        dg.addDataSet(hist2d_integral, 2);
        dg.addDataSet(hist2d_timeMax, 3);
        dg.addDataSet(hist2d_leadingEdgeTime, 4);
        dg.addDataSet(hist2d_timeOverThreshold, 5);
        dg.addDataSet(hist2d_constantFractionTime, 6);
        dg.addDataSet(hist2d_raw_occupancy, 7);
        dg.addDataSet(hist2d_raw_adcMax, 8);
        dg.addDataSet(hist2d_raw_integral, 9);
        dg.addDataSet(hist2d_raw_timeMax, 10);
        dg.addDataSet(hist2d_raw_leadingEdgeTime, 11);
        dg.addDataSet(hist2d_raw_timeOverThreshold, 12);
        dg.addDataSet(hist2d_raw_constantFractionTime, 13);
        dg.addDataSet(hits_vs_layer, 14);
        dg.addDataSet(occupancy_vs_layer, 15);
        dg.addDataSet(hist2d_8layer_hits, 16);
        dg.addDataSet(hist2d_8layer_occ, 17);
        dg.addDataSet(hist2d_7layer_hits, 18);
        dg.addDataSet(hist2d_6layer_hits, 21);
        dg.addDataSet(missing_layer_hit, 19);
        dg.addDataSet(missing_layer_hit6, 20);
        dg.addDataSet(missing_layer_hit5, 22);
        dg.addDataSet(number_of_layers_hit, 23);
        dg.addDataSet(tdc_values, 24);
        dg.addDataSet(time_values, 25);
        dg.addDataSet(waveform_time, 26);
        dg.addDataSet(average_waveform, 27);
        dg.addDataSet(average_waveform_raw, 28);
        dg.addDataSet(average_waveform_sample_count, 29); 
        dg.addDataSet(average_waveform_samples, 30); 
        dg.addDataSet(hist2d_wftime, 31);
        dg.addDataSet(hist2d_raw_wftime, 32);
        dg.addDataSet(hist1d_integral, 34);
        dg.addDataSet(hist1d_leadingEdgeTime, 33);
        dg.addDataSet(hist1d_timeOverThreshold, 35);
        dg.addDataSet(hist1d_constantFractionTime, 36);
        dg.addDataSet(hist1d_wftime, 37);
        dg.addDataSet(hist1d_adcMax, 38);
	this.getDataGroup().add(dg,0,0,0);
    }

    @Override
    public void plotHistos() {
        // plotting histos
        this.getDetectorCanvas().getCanvas("charge").cd(0);
        //this.getDetectorCanvas().getCanvas("charge").getPad(0).getAxisZ().getRange().setMinMax(0.0,0.01);
        //this.getDetectorCanvas().getCanvas("charge").getPad(0).getAxisZ().setAutoScale(false);
        //this.getDetectorCanvas().getCanvas("charge").getPad(0).getAxisZ().getRange().setMinMax(0.0,0.1);
        this.getDetectorCanvas().getCanvas("charge").draw(this.getDataGroup().getItem(0,0,0).getH2F("occupancy"));
        this.getDetectorCanvas().getCanvas("charge").cd(1);
        this.getDetectorCanvas().getCanvas("charge").draw(this.getDataGroup().getItem(0,0,0).getH2F("adcMax"));
        this.getDetectorCanvas().getCanvas("charge").cd(2);
        //this.getDetectorCanvas().getCanvas("charge").getPad(2).getAxisZ().setLog(true);
        this.getDetectorCanvas().getCanvas("charge").draw(this.getDataGroup().getItem(0,0,0).getH2F("integral"));
        this.getDetectorCanvas().getCanvas("charge").cd(3);
        //this.getDetectorCanvas().getCanvas("charge").getPad(2).getAxisZ().setLog(getLogZ());
        this.getDetectorCanvas().getCanvas("charge").draw(this.getDataGroup().getItem(0,0,0).getH2F("timeOverThreshold"));
        this.getDetectorCanvas().getCanvas("charge").update();

        this.getDetectorCanvas().getCanvas("time").cd(0);
        //this.getDetectorCanvas().getCanvas("time").getPad(0).getAxisZ().setLog(getLogZ());
        this.getDetectorCanvas().getCanvas("time").draw(this.getDataGroup().getItem(0,0,0).getH2F("timeMax"));
        this.getDetectorCanvas().getCanvas("time").cd(1);
        //this.getDetectorCanvas().getCanvas("time").getPad(1).getAxisZ().setLog(getLogZ());
        this.getDetectorCanvas().getCanvas("time").draw(this.getDataGroup().getItem(0,0,0).getH2F("leadingEdgeTime"));
        this.getDetectorCanvas().getCanvas("time").cd(2);
        ///this.getDetectorCanvas().getCanvas("time").getPad(3).getAxisZ().setLog(getLogZ());
        this.getDetectorCanvas().getCanvas("time").draw(this.getDataGroup().getItem(0,0,0).getH2F("constantFractionTime"));
        this.getDetectorCanvas().getCanvas("time").cd(3);
        ///this.getDetectorCanvas().getCanvas("time").getPad(3).getAxisZ().setLog(getLogZ());
        this.getDetectorCanvas().getCanvas("time").draw(this.getDataGroup().getItem(0,0,0).getH2F("wftime2d"));
        this.getDetectorCanvas().getCanvas("time").update();

        this.getDetectorCanvas().getCanvas("time1D").cd(0);
        ///this.getDetectorCanvas().getCanvas("time1D").getPad(3).getAxisZ().setLog(getLogZ());
        this.getDetectorCanvas().getCanvas("time1D").draw(this.getDataGroup().getItem(0,0,0).getH1F("adcMax1D"));
        this.getDetectorCanvas().getCanvas("time1D").cd(2);
        //this.getDetectorCanvas().getCanvas("time1D").getPad(0).getAxisZ().setLog(getLogZ());
        this.getDetectorCanvas().getCanvas("time1D").draw(this.getDataGroup().getItem(0,0,0).getH1F("integral1D"));
        this.getDetectorCanvas().getCanvas("time1D").cd(4);
        ///this.getDetectorCanvas().getCanvas("time1D").getPad(3).getAxisZ().setLog(getLogZ());
        this.getDetectorCanvas().getCanvas("time1D").draw(this.getDataGroup().getItem(0,0,0).getH1F("timeOverThreshold1D"));
        this.getDetectorCanvas().getCanvas("time1D").cd(1);
        //this.getDetectorCanvas().getCanvas("time1D").getPad(1).getAxisZ().setLog(getLogZ());
        this.getDetectorCanvas().getCanvas("time1D").draw(this.getDataGroup().getItem(0,0,0).getH1F("leadingEdgeTime1D"));
        this.getDetectorCanvas().getCanvas("time1D").cd(3);
        ///this.getDetectorCanvas().getCanvas("time1D").getPad(3).getAxisZ().setLog(getLogZ());
        this.getDetectorCanvas().getCanvas("time1D").draw(this.getDataGroup().getItem(0,0,0).getH1F("constantFractionTime1D"));
        this.getDetectorCanvas().getCanvas("time1D").cd(5);
        ///this.getDetectorCanvas().getCanvas("time1D").getPad(3).getAxisZ().setLog(getLogZ());
        this.getDetectorCanvas().getCanvas("time1D").draw(this.getDataGroup().getItem(0,0,0).getH1F("wftime1D"));
        this.getDetectorCanvas().getCanvas("time1D").update();

        this.getDetectorCanvas().getCanvas("Occupancy").cd(0);
        this.getDetectorCanvas().getCanvas("Occupancy").draw(this.getDataGroup().getItem(0,0,0).getH1F("occupancy_vs_layer"));
        this.getDetectorCanvas().getCanvas("Occupancy").cd(1);
        this.getDetectorCanvas().getCanvas("Occupancy").draw(this.getDataGroup().getItem(0,0,0).getH1F("number_of_layers_hit"));
        this.getDetectorCanvas().getCanvas("Occupancy").cd(2);
        this.getDetectorCanvas().getCanvas("Occupancy").draw(this.getDataGroup().getItem(0,0,0).getH1F("missing_layer_hit"));
        this.getDetectorCanvas().getCanvas("Occupancy").cd(3);
        this.getDetectorCanvas().getCanvas("Occupancy").draw(this.getDataGroup().getItem(0,0,0).getH2F("hist2d_7layer_hits"));
        //this.getDetectorCanvas().getCanvas("Occupancy").draw(this.getDataGroup().getItem(0,0,0).getH2F("hist2d_8layer_hits"));
        this.getDetectorCanvas().getCanvas("Occupancy").cd(4);
        //this.getDetectorCanvas().getCanvas("Occupancy").draw(this.getDataGroup().getItem(0,0,0).getH1F("missing_layer_hit6"));
        this.getDetectorCanvas().getCanvas("Occupancy").draw(this.getDataGroup().getItem(0,0,0).getH1F("missing_layer_hit5"));
        this.getDetectorCanvas().getCanvas("Occupancy").cd(5);
        this.getDetectorCanvas().getCanvas("Occupancy").draw(this.getDataGroup().getItem(0,0,0).getH2F("hist2d_6layer_hits"));


        this.getDetectorCanvas().getCanvas("1D").cd(0);
        this.getDetectorCanvas().getCanvas("1D").draw(this.getDataGroup().getItem(0,0,0).getH1F("tdc_values"));
        this.getDetectorCanvas().getCanvas("1D").cd(1);
        this.getDetectorCanvas().getCanvas("1D").draw(this.getDataGroup().getItem(0,0,0).getH1F("time_values"));
        this.getDetectorCanvas().getCanvas("1D").cd(2);
        this.getDetectorCanvas().getCanvas("1D").draw(this.getDataGroup().getItem(0,0,0).getH1F("average_waveform_samples"));
        this.getDetectorCanvas().getCanvas("1D").cd(3);
        this.getDetectorCanvas().getCanvas("1D").draw(this.getDataGroup().getItem(0,0,0).getH1F("waveform_time"));
        this.getDetectorCanvas().getCanvas("1D").cd(4);
        this.getDetectorCanvas().getCanvas("1D").draw(this.getDataGroup().getItem(0,0,0).getH1F("average_waveform"));

        this.getDetectorView().getView().repaint();
        this.getDetectorView().update();
    }

    @Override
    public void processEvent(DataEvent event) {
        // process event info and save into data group
        if(event.hasBank("AHDC::adc")==true){
            //System.out.println(" has AHDC bank!");
            DataBank bank = event.getBank("AHDC::adc");
            int rows = bank.rows();

            DataBank wfbank = event.getBank("AHDC::wf");
            int wfrows = wfbank.rows();

            // loop over hits and fill histograms
            for(int loop = 0; loop < wfrows; loop++){
                int sector     = wfbank.getByte("sector", loop);
                int layer      = wfbank.getByte("layer", loop);
                int comp       = wfbank.getShort("component", loop);
                int order      = wfbank.getByte("order", loop);
                long timestamp = wfbank.getLong("timestamp", loop);
                int  time      = wfbank.getInt("time", loop);

                if(  AHDCmonitor.getLayerNumber(layer)  > 8 ) continue;
                //System.out.println("wf timestamp: " +  timestamp);

                //H1F waveform_timestamp  = this.getDataGroup().getItem(0,0,0).getH1F("waveform_timestamp");
                //waveform_timestamp.fill(timestamp);

                H1F waveform_time  = this.getDataGroup().getItem(0,0,0).getH1F("waveform_time");
                waveform_time.fill(time);
                this.getDataGroup().getItem(0,0,0).getH2F("raw_wftime2d").fill(comp, AHDCmonitor.getLayerNumber(layer), time);
                this.getDataGroup().getItem(0,0,0).getH1F("wftime1D").fill(time);

                H1F average_waveform_raw  = this.getDataGroup().getItem(0,0,0).getH1F("average_waveform_raw");
                H1F average_waveform_sample_count  = this.getDataGroup().getItem(0,0,0).getH1F("average_waveform_sample_count");
                H1F average_waveform_samples  = this.getDataGroup().getItem(0,0,0).getH1F("average_waveform_samples");
                int n_samples = 0;
                for(int i= 0; i<49 ; i++) {
                    int sample_value = wfbank.getShort("s"+(i+1), loop);
                    average_waveform_raw.fill(i,sample_value);
                    if(sample_value >0) {
                //System.out.println("wf sample: " +i + ", " + sample_value  );
                        n_samples++;
                        average_waveform_sample_count.fill(i);
                    }
                }
                average_waveform_samples.fill(n_samples);
                waveform_time.fill(time);
            } //waveform loop

            //System.out.println("Derp\n");


            int layers_hit[] = {0,0,0,0,0,0,0,0};
            // loop over hits to see how many layers fire
            for(int loop = 0; loop < rows; loop++){
                int layer   = bank.getByte("layer", loop);
                int adc     = bank.getInt("ADC", loop);
                float time  = bank.getFloat("time", loop);
                if(adc>=0 && time>0) {
                    int layer_number = AHDCmonitor.getLayerNumber(layer);
                    layers_hit[layer_number-1]++;
                }
            }
            boolean all_layers = (AHDCmonitor.allLayersHit(layers_hit) > 0);

            H1F missing_layer_hit  = this.getDataGroup().getItem(0,0,0).getH1F("missing_layer_hit");;
            H1F missing_layer_hit6 = this.getDataGroup().getItem(0,0,0).getH1F("missing_layer_hit6");;
            H1F missing_layer_hit5 = this.getDataGroup().getItem(0,0,0).getH1F("missing_layer_hit5");;
            H1F number_of_layers_hit = this.getDataGroup().getItem(0,0,0).getH1F("number_of_layers_hit");;

            //System.out.println("layers_hit: " 
            //        + layers_hit[0] + " " 
            //        + layers_hit[1] + " "  
            //        + layers_hit[2] + " " 
            //        + layers_hit[3] + " "  
            //        + layers_hit[4] + " " 
            //        + layers_hit[5] + " "  
            //        + layers_hit[6] + " " 
            //        + layers_hit[7] + "  nlayersHit:" +  AHDCmonitor.nLayersHit(layers_hit));

            int n_layers_hit = AHDCmonitor.nLayersHit(layers_hit);
            number_of_layers_hit.fill(n_layers_hit);
            // Here we have 7/8 layers hit
            if(n_layers_hit >= 7){
                for(int il = 0 ;il<8; il++) {
                    if(layers_hit[il]==0){
                        missing_layer_hit.fill(il+1);
                    }
                }
            }
            if(n_layers_hit == 6){
                for(int il = 0 ;il<8; il++) {
                    if(layers_hit[il]==0){
                        missing_layer_hit6.fill(il+1);
                    }
                }
            }
            if((n_layers_hit >= 3) && (n_layers_hit <= 5)){
                for(int il = 0 ;il<8; il++) {
                    if(layers_hit[il]==0){
                        missing_layer_hit5.fill(il+1);
                    }
                }
            }

            // loop over hits and fill histograms
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

                //System.out.println("ROW " + loop + " SECTOR = " + sector + " LAYER = " + layer + " COMPONENT = " + comp + " ORDER + " + order +
                //      " ADC = " + adc + " TIME = " + time + "leadingEdgeTime = " + leadingEdgeTime); 
                if(adc>=0 && time>0) {
                    this.getDataGroup().getItem(0,0,0).getH1F("tdc_values").fill(leadingEdgeTime);
                    this.getDataGroup().getItem(0,0,0).getH1F("time_values").fill(time);

                    int layer_number = AHDCmonitor.getLayerNumber(layer);

                    // \todo move look up of histograms outside of loop
                    this.getDetectorSummary().getH2F("summary").fill(comp, layer_number);
                    this.getDataGroup().getItem(0,0,0).getH2F("raw_occupancy").fill(comp, layer_number);
                    this.getDataGroup().getItem(0,0,0).getH2F("raw_adcMax").fill(comp, layer_number, adc);
                    this.getDataGroup().getItem(0,0,0).getH2F("raw_integral").fill(comp, layer_number, integral);
                    this.getDataGroup().getItem(0,0,0).getH2F("raw_timeMax").fill(comp, layer_number, time);
                    this.getDataGroup().getItem(0,0,0).getH2F("raw_leadingEdgeTime").fill(comp, layer_number, leadingEdgeTime);
                    this.getDataGroup().getItem(0,0,0).getH2F("raw_timeOverThreshold").fill(comp, layer_number, timeOverThreshold);
                    this.getDataGroup().getItem(0,0,0).getH2F("raw_constantFractionTime").fill(comp, layer_number, constantFractionTime);
                    this.getDataGroup().getItem(0,0,0).getH1F("leadingEdgeTime1D").fill(leadingEdgeTime);
                    this.getDataGroup().getItem(0,0,0).getH1F("timeOverThreshold1D").fill(timeOverThreshold);
                    this.getDataGroup().getItem(0,0,0).getH1F("constantFractionTime1D").fill(constantFractionTime);
                    this.getDataGroup().getItem(0,0,0).getH1F("adcMax1D").fill(adc);
                    this.getDataGroup().getItem(0,0,0).getH1F("integral1D").fill(integral);

                    this.getDataGroup().getItem(0,0,0).getH1F("hits_vs_layer").fill(layer_number);
                    //this.getDataGroup().getItem(0,0,0).getH1F("occupancy_vs_layer").fill(layer_number);
                    if(all_layers){
                        this.getDataGroup().getItem(0,0,0).getH2F("hist2d_8layer_hits").fill(comp, layer_number);
                    }
                    //int n_layer_hit = AHDCmonitor.nLayersHit(layers_hit);
                    if(n_layers_hit == 7){
                        this.getDataGroup().getItem(0,0,0).getH2F("hist2d_7layer_hits").fill(comp, layer_number);
                    }
                    if((n_layers_hit >= 3) && (n_layers_hit <= 5)){
                        this.getDataGroup().getItem(0,0,0).getH2F("hist2d_6layer_hits").fill(comp, layer_number);
                    }

                }


            } // loop over hits


        }
    }

    @Override
    public void analysisUpdate() {
        int nevents = this.getNumberOfEvents();
        if(nevents>0) {

            H1F occupancy_vs_layer = this.getDataGroup().getItem(0,0,0).getH1F("occupancy_vs_layer");
            H1F hits_vs_layer      = this.getDataGroup().getItem(0,0,0).getH1F("hits_vs_layer");
            for(int ibin = 0; ibin < hits_vs_layer.getDataSize(0); ibin++){
                double layer_hits =  hits_vs_layer.getBinContent(ibin);
                occupancy_vs_layer.setBinContent(ibin, 100.0*layer_hits/(nevents*layer_wires[ibin]));
            }

            H1F average_waveform      = this.getDataGroup().getItem(0,0,0).getH1F("average_waveform");
            H1F average_waveform_raw  = this.getDataGroup().getItem(0,0,0).getH1F("average_waveform_raw");
            H1F average_waveform_sample_count   = this.getDataGroup().getItem(0,0,0).getH1F("average_waveform_sample_count");

            for(int ibin = 0; ibin < average_waveform_raw.getDataSize(0); ibin++){
                double sample_sum   =  average_waveform_raw.getBinContent(ibin);
                double sample_count =  average_waveform_sample_count.getBinContent(ibin);
                if(sample_count > 0){
                    average_waveform.setBinContent(ibin, sample_sum/(sample_count *nevents));
                }
            }


            H2F hist2d_raw_occupancy = this.getDataGroup().getItem(0,0,0).getH2F("raw_occupancy");
            for(int ibin = 0; ibin < hist2d_raw_occupancy.getDataBufferSize(); ibin++){
                float raw_occupancy = hist2d_raw_occupancy.getDataBufferBin(ibin);
                if (raw_occupancy != 0) {
                    // recover all raw values
                    // \todo: lookup the histograms before entering the loop over bins.
                    //
                    float raw_adcMax = this.getDataGroup().getItem(0,0,0).getH2F("raw_adcMax").getDataBufferBin(ibin);
                    float raw_integral = this.getDataGroup().getItem(0,0,0).getH2F("raw_integral").getDataBufferBin(ibin);
                    float raw_timeMax = this.getDataGroup().getItem(0,0,0).getH2F("raw_timeMax").getDataBufferBin(ibin);
                    float raw_leadingEdgeTime = this.getDataGroup().getItem(0,0,0).getH2F("raw_leadingEdgeTime").getDataBufferBin(ibin);
                    float raw_timeOverThreshold = this.getDataGroup().getItem(0,0,0).getH2F("raw_timeOverThreshold").getDataBufferBin(ibin);
                    float raw_constantFractionTime = this.getDataGroup().getItem(0,0,0).getH2F("raw_constantFractionTime").getDataBufferBin(ibin);
                    float raw_wftime2d = this.getDataGroup().getItem(0,0,0).getH2F("raw_wftime2d").getDataBufferBin(ibin);
                    // renormalise them
                    this.getDataGroup().getItem(0,0,0).getH2F("occupancy").setDataBufferBin(ibin, 100.0*raw_occupancy/nevents);
                    this.getDataGroup().getItem(0,0,0).getH2F("adcMax").setDataBufferBin(ibin, raw_adcMax/raw_occupancy);
                    this.getDataGroup().getItem(0,0,0).getH2F("integral").setDataBufferBin(ibin, raw_integral/raw_occupancy);
                    this.getDataGroup().getItem(0,0,0).getH2F("timeMax").setDataBufferBin(ibin, raw_timeMax/raw_occupancy);
                    this.getDataGroup().getItem(0,0,0).getH2F("leadingEdgeTime").setDataBufferBin(ibin, raw_leadingEdgeTime/raw_occupancy);
                    this.getDataGroup().getItem(0,0,0).getH2F("timeOverThreshold").setDataBufferBin(ibin, raw_timeOverThreshold/raw_occupancy);
                    this.getDataGroup().getItem(0,0,0).getH2F("constantFractionTime").setDataBufferBin(ibin, raw_constantFractionTime/raw_occupancy);
                    this.getDataGroup().getItem(0,0,0).getH2F("wftime2d").setDataBufferBin(ibin, raw_wftime2d/raw_occupancy);
                }
            }
        }
    }

}

