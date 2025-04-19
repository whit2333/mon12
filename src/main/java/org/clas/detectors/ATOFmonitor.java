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

/**
 *
 * @author
 */

public class ATOFmonitor extends DetectorMonitor {

  // Temporary storage for Bar TDCs per event
  private Map<Integer, Integer> barTDCOrder0Map;
  private Map<Integer, Integer> barTDCOrder1Map;

  private float tdc_bin_time = 0.015625f; // ns/bin

  public ATOFmonitor(String name) {
    super(name);
    // Add new tabs: "WedgeTDC", "BarTDC", "BarSumDiff"
    this.setDetectorTabNames("Module","Global Wedge", "Occupancy", "Wedge TDCs", "Bar TDCs", "Bar Sum and Diff","Wedge ToTs", "Bar ToTs", "Bar TW" );
    this.init(false);

    // Initialize temporary storage maps
    barTDCOrder0Map = new HashMap<>();
    barTDCOrder1Map = new HashMap<>();
  }

  @Override
  public void createHistos() {
    // Initialize canvas and create histograms
    this.setNumberOfEvents(0);

    // Module Canvas
    this.getDetectorCanvas().getCanvas("Module").divide(1, 3);
    this.getDetectorCanvas().getCanvas("Module").setGridX(false);
    this.getDetectorCanvas().getCanvas("Module").setGridY(false);

    // Module Canvas
    this.getDetectorCanvas().getCanvas("Global Wedge").divide(1, 3);
    this.getDetectorCanvas().getCanvas("Global Wedge").setGridX(false);
    this.getDetectorCanvas().getCanvas("Global Wedge").setGridY(false);

    // Occupancy Canvas
    this.getDetectorCanvas().getCanvas("Occupancy").divide(1, 1);
    this.getDetectorCanvas().getCanvas("Occupancy").setGridX(false);
    this.getDetectorCanvas().getCanvas("Occupancy").setGridY(false);

    //// WedgeScalers Canvas
    //this.getDetectorCanvas().getCanvas("Wedge Hits").divide(1, 1);
    //this.getDetectorCanvas().getCanvas("Wedge Hits").setGridX(false);
    //this.getDetectorCanvas().getCanvas("Wedge Hits").setGridY(false);

    //// BarScalers Canvas
    //this.getDetectorCanvas().getCanvas("Bar Hits").divide(1, 1);
    //this.getDetectorCanvas().getCanvas("Bar Hits").setGridX(false);
    //this.getDetectorCanvas().getCanvas("Bar Hits").setGridY(false);

    // WedgeTDC Canvas (3x5 grid for sectors 0-14)
    this.getDetectorCanvas().getCanvas("Wedge TDCs").divide(3, 5);
    this.getDetectorCanvas().getCanvas("Wedge TDCs").setGridX(false);
    this.getDetectorCanvas().getCanvas("Wedge TDCs").setGridY(false);

    // BarTDC Canvas (3x5 grid for sectors 0-14)
    this.getDetectorCanvas().getCanvas("Bar TDCs").divide(3, 5);
    this.getDetectorCanvas().getCanvas("Bar TDCs").setGridX(false);
    this.getDetectorCanvas().getCanvas("Bar TDCs").setGridY(false);

    // BarSumDiff Canvas
    this.getDetectorCanvas().getCanvas("Bar Sum and Diff").divide(2, 2);
    this.getDetectorCanvas().getCanvas("Bar Sum and Diff").setGridX(false);
    this.getDetectorCanvas().getCanvas("Bar Sum and Diff").setGridY(false);

    // WedgeTDC Canvas (3x5 grid for sectors 0-14)
    this.getDetectorCanvas().getCanvas("Wedge ToTs").divide(3, 5);
    this.getDetectorCanvas().getCanvas("Wedge ToTs").setGridX(false);
    this.getDetectorCanvas().getCanvas("Wedge ToTs").setGridY(false);

    // BarTDC Canvas (3x5 grid for sectors 0-14)
    this.getDetectorCanvas().getCanvas("Bar ToTs").divide(3, 5);
    this.getDetectorCanvas().getCanvas("Bar ToTs").setGridX(false);
    this.getDetectorCanvas().getCanvas("Bar ToTs").setGridY(false);

    // BarTDC Canvas (3x5 grid for sectors 0-14)
    this.getDetectorCanvas().getCanvas("Bar TW").divide(4, 2);
    this.getDetectorCanvas().getCanvas("Bar TW").setGridX(false);
    this.getDetectorCanvas().getCanvas("Bar TW").setGridY(false);

    // Update summary histogram
    H1F summary = new H1F("summary", "summary", 15, -0.5, 14.5);
    summary.setTitleX("Sector");
    summary.setTitleY("ATOF Hits");
    summary.setTitle("ATOF Summary");
    summary.setFillColor(36);

    // DataGroup has a (int row, int cols) constructor where the ros and cols do nothing!
    DataGroup sum = new DataGroup();
    sum.addDataSet(summary, 0);
    this.setDetectorSummary(sum); // special summary plots?

    String run_number_stub = " [run:" + runNumber + "]";

    // -----------------------------------------------------------------------------------
    H1F module_wedge_hits = new H1F("module_wedge_hits", "module wedge hits "+run_number_stub, 15, -0.5, 14.5);
    module_wedge_hits.setTitleX("Module");
    module_wedge_hits.setTitleY("ATOF wedge Hits");
    module_wedge_hits.setFillColor(36);

    H1F module_bar_hits = new H1F("module_bar_hits", "module bar hits"+run_number_stub, 15, -0.5, 14.5);
    module_bar_hits.setTitleX("Module");
    module_bar_hits.setTitleY("ATOF bar Hits");
    module_bar_hits.setFillColor(37);

    H2F module_vs_z_wedge_hits = new H2F("module_vs_z_wedge_hits", "Module vs Z_{wedge}"+run_number_stub, 15, -0.5, 14.5,12,-0.5,11.5);
    module_vs_z_wedge_hits.setTitleX("Module");
    module_vs_z_wedge_hits.setTitleY("comp+order");
    module_vs_z_wedge_hits.setTitle("Z vs module ");

    DataGroup module_group = new DataGroup();
    module_group.addDataSet(module_wedge_hits, 0);
    module_group.addDataSet(module_bar_hits, 1);
    module_group.addDataSet(module_vs_z_wedge_hits, 2);
    this.getDataGroup().add(module_group, 0, 0, 0); // tab 0 group

    // -----------------------------------------------------------------------------------

    // Update globalwedge 
    H1F globalwedge_wedge_hits = new H1F("globalwedge_wedge_hits", "globalwedge wedge hits"+run_number_stub, 60, -0.5, 59.5);
    globalwedge_wedge_hits.setTitleX("global wedge");
    globalwedge_wedge_hits.setTitleY("ATOF wedge Hits");
    globalwedge_wedge_hits.setFillColor(36);

    H1F globalwedge_bar_hits = new H1F("globalwedge_bar_hits", "globalwedge bar hits"+run_number_stub, 60, -0.5, 59.5);
    globalwedge_bar_hits.setTitleX("global wedge");
    globalwedge_bar_hits.setTitleY("ATOF bar Hits");
    globalwedge_bar_hits.setFillColor(37);

    H2F globalwedge_wedge_vs_z_hits = new H2F("globalwedge_wedge_vs_z_hits", "globalwedge wedge hits"+run_number_stub, 60, -0.5, 59.5,10,-0.5,9.5);
    globalwedge_wedge_vs_z_hits.setTitleX("global wedge");
    globalwedge_wedge_vs_z_hits.setTitleY("Z_{wedge}");
    globalwedge_wedge_vs_z_hits.setTitle("Z vs Global Wedge");

    H2F globalwedge_all_vs_z_hits = new H2F("globalwedge_all_vs_z_hits", "globalwedge all hits"+run_number_stub, 60, -0.5, 59.5,12,-0.5,11.5);
    globalwedge_wedge_vs_z_hits.setTitleX("global wedge");
    globalwedge_wedge_vs_z_hits.setTitleY("component + order");

    H2F globalwedge_all_vs_z_occ = new H2F("globalwedge_all_vs_z_occ", "globalwedge occupancy"+run_number_stub, 60, -0.5, 59.5,12,-0.5,11.5);
    globalwedge_all_vs_z_occ.setTitleX("global wedge (phi)");
    globalwedge_all_vs_z_occ.setTitleY("component + order");

    DataGroup globalwedge_group = new DataGroup();
    globalwedge_group.addDataSet(globalwedge_wedge_hits, 0);
    globalwedge_group.addDataSet(globalwedge_bar_hits, 1);
    globalwedge_group.addDataSet(globalwedge_wedge_vs_z_hits, 2);
    globalwedge_group.addDataSet(globalwedge_all_vs_z_hits, 3);
    globalwedge_group.addDataSet(globalwedge_all_vs_z_occ, 4);

    this.getDataGroup().add(globalwedge_group, 2, 0, 0);  // tab 2 group 


    // Existing histograms
    H2F rawTDC = new H2F("rawTDC", "rawTDC", 6, 0.5, 6.5, 8, 0.5, 8.5);
    rawTDC.setTitleY("Component");
    rawTDC.setTitleX("Layer");

    H2F occTDC = new H2F("occTDC", "occTDC", 6, 0.5, 6.5, 8, 0.5, 8.5);
    occTDC.setTitleY("Component");
    occTDC.setTitleX("Layer");
    occTDC.setTitle("TDC Occupancy");

    H1F occTDC1D = new H1F("occTDC1D", "occTDC1D", 10, -0.5, 9.5);
    occTDC1D.setTitleX("Scintillator (Scintillator/Layer)");
    occTDC1D.setTitleY("Counts");
    occTDC1D.setTitle("TDC Occupancy");
    occTDC1D.setFillColor(3);

    H2F tdc = new H2F("tdc", "tdc", 300, 167500, 172000, 8, 0.5, 8.5);
    tdc.setTitleX("TDC - Value");
    tdc.setTitleY("Scintillator");

    // WedgeScalers Histogram
    H2F wedgeScalers = new H2F("wedgeScalers", "Wedge Scalers"+run_number_stub, 60, -0.5, 59.5, 10, -0.5, 9.5);
    wedgeScalers.setTitleX("Wedge Row (Azimuth)");
    wedgeScalers.setTitleY("Wedge Column (z)");
    wedgeScalers.setTitle("Wedge Scalers");
    // Uncomment if you have a method to set the palette
    // wedgeScalers.setOptStat(0); // Disable statistics box

    // BarScalers Histogram
    H2F barScalers = new H2F("barScalers", "Bar Scalers"+run_number_stub, 60, -0.5, 59.5, 2, -0.5, 1.5);
    barScalers.setTitleX("Bar Row (Azimuth)");
    barScalers.setTitleY("Bar End (z)");
    barScalers.setTitle("Bar Scalers");
    // Uncomment if you have a method to set the palette
    // barScalers.setOptStat(0); // Disable statistics box

    // WedgeTDC Histograms (3x5 grid for sectors 0-14)
    DataGroup wedgeTDCGroup = new DataGroup();
    for (int sector = 0; sector < 15; sector++) {
      String histName = "wedgeTDC_sector_" + sector;
      H2F wedgeTDC = new H2F(histName, "Wedge TDC Sector " + sector+run_number_stub, 100, 150, 250,40,-0.5,39.5);
      wedgeTDC.setTitleX("TDC [ns]");
      wedgeTDC.setTitleY("wedge_i");
      wedgeTDC.setTitle("M" + sector + " Wedge TDCs"+run_number_stub);
      // Uncomment if you have a method to disable statistics box
      // wedgeTDC.setOptStat(0);
      wedgeTDCGroup.addDataSet(wedgeTDC, sector);
    }

    // BarTDC Histograms (3x5 grid for sectors 0-14)
    DataGroup barTDCGroup = new DataGroup();
    for (int sector = 0; sector < 15; sector++) {
      String histName = "barTDC_sector_" + sector;
      H2F barTDC = new H2F(histName, "Bar TDC Sector " + sector+run_number_stub, 100, 150, 250,8,-0.5,7.5);
      barTDC.setTitleX("TDC [ns]");
      barTDC.setTitleY("bar_i");
      barTDC.setTitle("M" + sector + " Bar TDCs"+run_number_stub);
      // Uncomment if you have a method to disable statistics box
      // barTDC.setOptStat(0);
      barTDCGroup.addDataSet(barTDC, sector);
    }

    // WedgeToT Histograms (3x5 grid for sectors 0-14)
    DataGroup wedgeToTGroup = new DataGroup();
    for (int sector = 0; sector < 15; sector++) {
      String histName = "wedgeToT_sector_" + sector;
      H2F wedgeToT = new H2F(histName, "Wedge ToT Sector " + sector, 200, 0, 200,40,-0.5,39.5);
      wedgeToT.setTitleX("ToT [ns]");
      wedgeToT.setTitleY("wedge_i");
      wedgeToT.setTitle("M" + sector + " Wedge ToTs"+run_number_stub);
      // Uncomment if you have a method to disable statistics box
      // wedgeToT.setOptStat(0);
      wedgeToTGroup.addDataSet(wedgeToT, sector);
    }

    // BarToT Histograms (3x5 grid for sectors 0-14)
    DataGroup barToTGroup = new DataGroup();
    for (int sector = 0; sector < 15; sector++) {
      String histName = "barToT_sector_" + sector;
      H2F barToT = new H2F(histName, "Bar ToT Sector " + sector, 200, 0, 200,8,-0.5,7.5);
      barToT.setTitleX("ToT [ns]");
      barToT.setTitleY("bar_i");
      barToT.setTitle("M" + sector + " Bar ToTs"+run_number_stub);
      // Uncomment if you have a method to disable statistics box
      // barToT.setOptStat(0);
      barToTGroup.addDataSet(barToT, sector);
    }

    // BarToT Histograms (3x5 grid for sectors 0-14)
    DataGroup barTWGroup = new DataGroup();
    for (int sector = 0; sector < 15; sector++) {
        for (int chan = 0; chan < 8; chan++) {
            String histName = "barTW_sector_" + sector +"_ch"+chan;
            H2F barTW = new H2F(histName, "Bar TW Sector " + sector +"_ch"+chan,  200, 0, 200, 100, 150, 250);
            barTW.setTitleX("ToT [ns]");
            barTW.setTitleY("TDC [ns]");
            barTW.setTitle("M" + sector + " ch" + chan + " Bar TW"+run_number_stub);
            // Uncomment if you have a method to disable statistics box
            // barToT.setOptStat(0);
            barTWGroup.addDataSet(barTW, sector*15+chan );
        }
    }

    // BarSumDiff Histograms
    int barsum_peak = 36000; // peak location used to center the histogram binning:
    H1F barSum = new H1F("barSum", "Bar TDC Sum", 200, barsum_peak-20000, barsum_peak+20000); // Assuming sum range
    barSum.setTitleX("Sum of TDCs (Order0 + Order1)");
    barSum.setTitleY("Counts");
    barSum.setFillColor(46);
    // Uncomment if you have a method to disable statistics box
    // barSum.setOptStat(0);
    float barsumtime_peak = 2*36000*tdc_bin_time; // peak location used to center the histogram binning:
    H1F barSumTime = new H1F("barSumTime", "Bar Sum Time "+run_number_stub, 200, 0,2000);//barsum_peak*tdc_bin_time-20000*tdc_bin_time, barsum_peak*tdc_bin_time+20000*tdc_bin_time); // Assuming sum range
    barSumTime.setTitleX("Sum of Times (Order0 + Order1) [ns]");
    barSumTime.setTitleY("Counts");
    barSumTime.setFillColor(46);

    H1F barDiff = new H1F("barDiff", "Bar TDC Difference"+run_number_stub, 200, -20000, 20000); // Assuming difference range
    barDiff.setTitleX("Difference of TDCs (Order1 - Order0)");
    barDiff.setTitleY("Counts");
    barDiff.setFillColor(38);
    H1F barDiff2 = new H1F("barDiff2", "Bar TDC Diff per 1k events"+run_number_stub, 200, -20000, 20000); // Assuming difference range
    barDiff2.setTitleX("Difference of TDCs (Order1 - Order0)");
    barDiff2.setTitleY("Counts/1k events");
    barDiff2.setFillColor(38);

    H1F barDiffTime = new H1F("barDiffTime", "Bar TDC Difference"+run_number_stub, 200, -5, 7);
    barDiffTime.setTitleX("Difference of Times (Order1 - Order0) [ns]");
    barDiffTime.setTitleY("Counts");
    barDiffTime.setFillColor(38);
    // Uncomment if you have a method to disable statistics box
    // barDiff.setOptStat(0);

    // Create data group for existing and new histograms
    DataGroup dg = new DataGroup();
    dg.addDataSet(rawTDC, 0);
    dg.addDataSet(occTDC, 0);
    dg.addDataSet(occTDC1D, 0);
    dg.addDataSet(tdc, 0);
    dg.addDataSet(wedgeScalers, 0);
    dg.addDataSet(barScalers, 0);
    this.getDataGroup().add(dg, 1, 0, 0); // tab group 1

    // Create data groups for new histograms
    this.getDataGroup().add(wedgeTDCGroup, 4, 0, 0); // Tab index 4: "WedgeTDC"
    this.getDataGroup().add(barTDCGroup, 5, 0, 0);   // Tab index 5: "BarTDC"
    this.getDataGroup().add(wedgeToTGroup, 4, 1, 0); 
    this.getDataGroup().add(barToTGroup, 5, 1, 0);   
    this.getDataGroup().add(barTWGroup, 7, 0, 0);   

    DataGroup barSumDiffGroup = new DataGroup();
    barSumDiffGroup.addDataSet(barSum, 0);
    barSumDiffGroup.addDataSet(barDiff, 1);
    barSumDiffGroup.addDataSet(barSumTime, 2);
    barSumDiffGroup.addDataSet(barDiffTime, 3);
    barSumDiffGroup.addDataSet(barDiff2, 4);
    this.getDataGroup().add(barSumDiffGroup, 6, 0, 0); // Tab index 6: "BarSumDiff"
  }

  @Override
  public void plotHistos() {

    // Plotting existing histograms
    this.getDetectorCanvas().getCanvas("Module").cd(0);
    this.getDetectorCanvas().getCanvas("Module").draw(this.getDataGroup().getItem(0, 0, 0).getH1F("module_bar_hits"));
    this.getDetectorCanvas().getCanvas("Module").cd(1);
    this.getDetectorCanvas().getCanvas("Module").getPad(1).setPalette("kCool");
    this.getDetectorCanvas().getCanvas("Module").getPad(1).getAxisZ().setLog(true);
    this.getDetectorCanvas().getCanvas("Module").draw(this.getDataGroup().getItem(0, 0, 0).getH2F("module_vs_z_wedge_hits"));
    this.getDetectorCanvas().getCanvas("Module").cd(2);
    this.getDetectorCanvas().getCanvas("Module").draw(this.getDataGroup().getItem(0, 0, 0).getH1F("module_wedge_hits"));

    // Plotting existing histograms
    EmbeddedCanvas can = this.getDetectorCanvas().getCanvas("Global Wedge");
    can.cd(0);
    can.draw(this.getDataGroup().getItem(2, 0, 0).getH1F("globalwedge_bar_hits"));
    can.getPad(0).getAxisY().setLog(true);
    can.cd(1);
    can.getPad(1).getAxisY().setLog(true);
    can.draw(this.getDataGroup().getItem(2, 0, 0).getH1F("globalwedge_wedge_hits"));
    can.cd(2);
    can.getPad(2).setPalette("kCool");
    can.getPad(2).getAxisZ().setLog(true);
    can.draw(this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_wedge_vs_z_hits"));

    //this.getDetectorCanvas().getCanvas("occupancy").getPad(0).setPalette("kCool");
    //this.getDetectorCanvas().getCanvas("occupancy").getPad(0).getAxisZ().setLog(getLogZ());
    //this.getDetectorCanvas().getCanvas("occupancy").draw(this.getDataGroup().getItem(1, 0, 0).getH2F("occTDC"));

    // Plotting existing histograms
    this.getDetectorCanvas().getCanvas("Occupancy").cd(0);
    // Optionally, set the palette if supported
    this.getDetectorCanvas().getCanvas("Occupancy").getPad(0).setPalette("kCool");
    //this.getDetectorCanvas().getCanvas("occupancy").getPad(0).getAxisZ().setLog(true);
    //this.getDetectorCanvas().getCanvas("occupancy").draw(this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_all_vs_z_hits"));
    this.getDetectorCanvas().getCanvas("Occupancy").draw(this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_all_vs_z_occ"));

    //this.getDetectorCanvas().getCanvas("occupancy").cd(1);
    //// Optionally, set the palette if supported
    //this.getDetectorCanvas().getCanvas("occupancy").getPad(1).setPalette("kCool");
    //this.getDetectorCanvas().getCanvas("occupancy").getPad(1).getAxisY().setLog(true);
    //this.getDetectorCanvas().getCanvas("occupancy").draw(this.getDataGroup().getItem(1, 0, 0).getH1F("occTDC1D"));
    //this.getDetectorCanvas().getCanvas("occupancy").update();

    //this.getDetectorCanvas().getCanvas("tdc").cd(0);
    //// Optionally, set the palette if supported
    //this.getDetectorCanvas().getCanvas("tdc").getPad(0).setPalette("kCool");
    //this.getDetectorCanvas().getCanvas("tdc").getPad(0).getAxisZ().setLog(getLogZ());
    //this.getDetectorCanvas().getCanvas("tdc").draw(this.getDataGroup().getItem(1, 0, 0).getH2F("tdc"));

    //// Plot WedgeScalers
    //this.getDetectorCanvas().getCanvas("Wedge Hits").cd(0);
    //this.getDetectorCanvas().getCanvas("Wedge Hits").getPad(0).setPalette("kCool");
    //this.getDetectorCanvas().getCanvas("Wedge Hits").draw(this.getDataGroup().getItem(1, 0, 0).getH2F("wedgeScalers"));
    //this.getDetectorCanvas().getCanvas("Wedge Hits").update();

    //// Plot BarScalers
    //this.getDetectorCanvas().getCanvas("Bar Hits").cd(0);
    //this.getDetectorCanvas().getCanvas("Bar Hits").getPad(0).setPalette("kCool");
    //this.getDetectorCanvas().getCanvas("Bar Hits").draw(this.getDataGroup().getItem(1, 0, 0).getH2F("barScalers"));
    //this.getDetectorCanvas().getCanvas("Bar Hits").update();

    // Plot WedgeTDC Histograms
    DataGroup wedgeTDCGroup = this.getDataGroup().getItem(4, 0, 0);
    for (int sector = 0; sector < 15; sector++) {
      this.getDetectorCanvas().getCanvas("Wedge TDCs").cd(sector);
      this.getDetectorCanvas().getCanvas("Wedge TDCs").getPad(sector).setPalette("kCool");
      H2F wedgeTDC = wedgeTDCGroup.getH2F("wedgeTDC_sector_" + sector);
      this.getDetectorCanvas().getCanvas("Wedge TDCs").draw(wedgeTDC);
      //this.getDetectorCanvas().getCanvas("Wedge TDCs").getPad(sector).getAxisY().setLog(true);
    }
    this.getDetectorCanvas().getCanvas("Wedge TDCs").update();

    // Plot BarTDC Histograms
    DataGroup barTDCGroup = this.getDataGroup().getItem(5, 0, 0);
    for (int sector = 0; sector < 15; sector++) {
      this.getDetectorCanvas().getCanvas("Bar TDCs").cd(sector);
      this.getDetectorCanvas().getCanvas("Bar TDCs").getPad(sector).setPalette("kCool");
      H2F barTDC = barTDCGroup.getH2F("barTDC_sector_" + sector);
      this.getDetectorCanvas().getCanvas("Bar TDCs").draw(barTDC);
      //this.getDetectorCanvas().getCanvas("Bar TDCs").getPad(sector).getAxisY().setLog(true);
    }
    this.getDetectorCanvas().getCanvas("Bar TDCs").update();

    // Plot BarSumDiff Histograms
    DataGroup barSumDiffGroup = this.getDataGroup().getItem(6, 0, 0);
    this.getDetectorCanvas().getCanvas("Bar Sum and Diff").cd(0);
    H1F barSum = barSumDiffGroup.getH1F("barSum");
    barSum.setFillColor(46);
    this.getDetectorCanvas().getCanvas("Bar Sum and Diff").draw(barSum);
    this.getDetectorCanvas().getCanvas("Bar Sum and Diff").getPad(0).getAxisY().setLog(true);

    this.getDetectorCanvas().getCanvas("Bar Sum and Diff").cd(1);
    H1F barDiff2 = barSumDiffGroup.getH1F("barDiff2");
    barDiff2.setFillColor(38);
    this.getDetectorCanvas().getCanvas("Bar Sum and Diff").draw(barDiff2);
    this.getDetectorCanvas().getCanvas("Bar Sum and Diff").update();
    this.getDetectorCanvas().getCanvas("Bar Sum and Diff").getPad(1).getAxisY().setLog(true);


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


    // Plot WedgeToT Histograms
    DataGroup wedgeToTGroup = this.getDataGroup().getItem(4, 1, 0);
    for (int sector = 0; sector < 15; sector++) {
      this.getDetectorCanvas().getCanvas("Wedge ToTs").cd(sector);
      this.getDetectorCanvas().getCanvas("Wedge ToTs").getPad(sector).setPalette("kCool");
      H2F wedgeToT = wedgeToTGroup.getH2F("wedgeToT_sector_" + sector);
      this.getDetectorCanvas().getCanvas("Wedge ToTs").draw(wedgeToT);
      //this.getDetectorCanvas().getCanvas("Wedge ToTs").getPad(sector).getAxisY().setLog(true);
    }
    this.getDetectorCanvas().getCanvas("Wedge ToTs").update();

    // Plot BarToT Histograms
    DataGroup barToTGroup = this.getDataGroup().getItem(5, 1, 0);
    for (int sector = 0; sector < 15; sector++) {
      this.getDetectorCanvas().getCanvas("Bar ToTs").cd(sector);
      this.getDetectorCanvas().getCanvas("Bar ToTs").getPad(sector).setPalette("kCool");
      H2F barToT = barToTGroup.getH2F("barToT_sector_" + sector);
      this.getDetectorCanvas().getCanvas("Bar ToTs").draw(barToT);
      //this.getDetectorCanvas().getCanvas("Bar ToTs").getPad(sector).getAxisY().setLog(true);
    }
    this.getDetectorCanvas().getCanvas("Bar ToTs").update();

    // Plot BarToT Histograms
    DataGroup barTWGroup = this.getDataGroup().getItem(7, 0, 0);
    for (int sector = 0; sector < 1; sector++) {
        for (int chan = 0; chan < 8; chan++) {
            String TWhistName = "barTW_sector_" + sector +"_ch"+ chan;
            this.getDetectorCanvas().getCanvas("Bar TW").cd(chan);
            this.getDetectorCanvas().getCanvas("Bar TW").getPad(chan).setPalette("kCool");
            H2F barTW = barTWGroup.getH2F(TWhistName);
            this.getDetectorCanvas().getCanvas("Bar TW").draw(barTW);
            //this.getDetectorCanvas().getCanvas("Bar TWs").getPad(sector).getAxisY().setLog(true);
        }
    }
    this.getDetectorCanvas().getCanvas("Bar TW").update();


    // Update detector view
    this.getDetectorView().getView().repaint();
    this.getDetectorView().update();
  }

  @Override
  public void processEvent(DataEvent event) {
    // Clear temporary storage at the start of each event
    barTDCOrder0Map.clear();
    barTDCOrder1Map.clear();

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
          this.getDataGroup().getItem(1, 0, 0).getH2F("occTDC").fill(comp, layer);
          this.getDataGroup().getItem(1, 0, 0).getH1F("occTDC1D").fill(wire);
          this.getDataGroup().getItem(1, 0, 0).getH2F("tdc").fill(tdc * 1.0, wire);
          this.getDetectorSummary().getH1F("summary").fill(sector);

          if(comp <10) {
            this.getDataGroup().getItem(0, 0, 0).getH1F("module_wedge_hits").fill(sector);

            this.getDataGroup().getItem(2, 0, 0).getH1F("globalwedge_wedge_hits").fill(w);
            this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_wedge_vs_z_hits").fill(w,comp);


          } else{
            this.getDataGroup().getItem(0, 0, 0).getH1F("module_bar_hits").fill(sector);

            this.getDataGroup().getItem(2, 0, 0).getH1F("globalwedge_bar_hits").fill(w);
          }

          this.getDataGroup().getItem(0, 0, 0).getH2F("module_vs_z_wedge_hits").fill(sector,comp+order);
          this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_all_vs_z_hits").fill(w,comp+order);
          //this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_all_vs_z_occ").fill(w,comp+order);




          int xbin = sector * 4 + layer ;
          // Fill Wedge Scalers histogram
          if (comp >= 0 && comp <= 9) {
            this.getDataGroup().getItem(1, 0, 0).getH2F("wedgeScalers").fill(xbin, comp);
            // Fill Wedge TDC histogram
            DataGroup wedgeTDCGroup = this.getDataGroup().getItem(4, 0, 0);
            DataGroup wedgeToTGroup = this.getDataGroup().getItem(4, 1, 0);
            if (sector < 15) { // Ensure sector is within 0-14
              H2F wedgeTDC = wedgeTDCGroup.getH2F("wedgeTDC_sector_" + sector);
              wedgeTDC.fill(tdc * tdc_bin_time, layer*10 + comp);
              H2F wedgeToT = wedgeToTGroup.getH2F("wedgeToT_sector_" + sector);
              wedgeToT.fill(tot * tdc_bin_time, layer*10 + comp);
            }
          }
          // Fill Bar Scalers histogram
          else if (comp == 10) {
            this.getDataGroup().getItem(1, 0, 0).getH2F("barScalers").fill(xbin, order);

            int chan = (order*4+comp);

            String TWhistName = "barTW_sector_" + sector +"_ch"+ chan;
            this.getDataGroup().getItem(7, 0, 0).getH2F(TWhistName).fill(tot * tdc_bin_time, tdc * tdc_bin_time);

            // Fill Bar TDC histogram
            DataGroup barTDCGroup = this.getDataGroup().getItem(5, 0, 0);
            DataGroup barToTGroup = this.getDataGroup().getItem(5, 1, 0);
            if (sector < 15) { // Ensure sector is within 0-14
              H2F barTDC = barTDCGroup.getH2F("barTDC_sector_" + sector);
              barTDC.fill(tdc * tdc_bin_time,order*4+layer);
              H2F barToT = barToTGroup.getH2F("barToT_sector_" + sector);
              barToT.fill(tot * tdc_bin_time,order*4+layer);
            }

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
  }

  @Override
  public void analysisUpdate() {
    if (this.getNumberOfEvents() > 0) {

      DataGroup barSumDiffGroup = this.getDataGroup().getItem(6, 0, 0);
      H1F barSum = barSumDiffGroup.getH1F("barSum");
      H1F barDiff = barSumDiffGroup.getH1F("barDiff");
      H1F barDiff2 = barSumDiffGroup.getH1F("barDiff2");
      H1F barSumTime = barSumDiffGroup.getH1F("barSumTime");
      H1F barDiffTime = barSumDiffGroup.getH1F("barDiffTime");
      double norm = 1.0e3/this.getNumberOfEvents();

      for (int loop = 0; loop < barDiff.getDataSize(0); loop++) {
          barDiff2.setBinContent(loop, barDiff.getBinContent(loop) *norm);
      }

      H2F hits = this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_all_vs_z_hits");
      for (int loop = 0; loop < hits.getDataBufferSize(); loop++) {
        this.getDataGroup().getItem(2, 0, 0).getH2F("globalwedge_all_vs_z_occ").setDataBufferBin(loop, 100.0*hits.getDataBufferBin(loop) / this.getNumberOfEvents());
      }
      //H2F raw = this.getDataGroup().getItem(1, 0, 0).getH2F("rawTDC");
      //for (int loop = 0; loop < raw.getDataBufferSize(); loop++) {
      //  this.getDataGroup().getItem(1, 0, 0).getH2F("occTDC").setDataBufferBin(loop, 100 * raw.getDataBufferBin(loop) / this.getNumberOfEvents());
      //}
    }
  }
}
