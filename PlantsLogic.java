package EECS1021.FinalProject;
import org.firmata4j.IODeviceEventListener;
import org.firmata4j.IOEvent;
import org.firmata4j.Pin;
import org.firmata4j.ssd1306.MonochromeCanvas;
import org.firmata4j.ssd1306.SSD1306;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;

public class PlantsLogic extends TimerTask implements IODeviceEventListener {
    private final Pin buttonPin;
    private final Pin MOSFETPin;
    private final Pin lightPin;
    private final SSD1306 display;
    private ArrayList<Long> wetValue = new ArrayList<>();
    private long cooldown;
    private final Pin sensorPin;
    public boolean Running = true;
    public final FileWriter fileWriter;


    public PlantsLogic(Pin buttonPin, Pin mosfetPin, Pin lightPin, Pin sensorPin, SSD1306 display, FileWriter fileWriter) {
        this.buttonPin = buttonPin;
        this.MOSFETPin = mosfetPin;
        this.lightPin = lightPin;
        this.sensorPin = sensorPin;
        this.display = display;
        this.fileWriter = fileWriter;
    }

    public static void initialize(SSD1306 display) throws InterruptedException {
        display.init();
        display.getCanvas().setTextsize(2);
        System.out.println("Initializing...");
        display.getCanvas().drawString(0,0,"Initializing...");
        display.display();
        Thread.sleep(1000);
        display.clear();

    }

    public static ArrayList<Long> ValuesArray(ArrayList<Long> SensorArray, long AverageSize, Pin SensorValue){

        if (SensorArray.size() < AverageSize ) { // Get array up to the propper size
            SensorArray.add(SensorValue.getValue());
        } else {
            SensorArray.add(SensorValue.getValue()); //Keep the array rolling
            SensorArray.removeFirst();
        }

      return(SensorArray);
    }

    public static long RollingAverage(ArrayList<Long> SensorArray, int lowerBound, int upperBound){
        long RollingAvg = 0;
        for (int i = 0; i <= SensorArray.size() - 1; i++){
            RollingAvg += (((SensorArray.get(i)- lowerBound)/((upperBound - lowerBound)/100))/ SensorArray.size());

            if ((SensorArray.get(i) < lowerBound || SensorArray.get(i) > upperBound)){
                System.out.println("NEW EXTREME VALUE "+SensorArray.get(i));
            }

        }

        if (RollingAvg > 100){
            RollingAvg = 100;
        } else if (RollingAvg < 0) {
            RollingAvg = 0;
        }

        return(RollingAvg);
    }

    public static long Watering(SSD1306 display, Pin MOSFETPin, long RollingAvg, long Threshold, long Cooldown , long CooldownConstant) throws InterruptedException, IOException {
        if (RollingAvg >= Threshold && Cooldown <= 0){
            display.clear();
            display.getCanvas().drawString(0, 0, "Watering...");
            display.display();
            MOSFETPin.setValue(1);
            Thread.sleep(1000);
            MOSFETPin.setValue(0);
            display.getCanvas().drawString(0, 0, "Watering complete :)");
            display.display();
            System.out.println("wet time");
            Cooldown = CooldownConstant;
        }
        return(Cooldown);
    }

    public static long Update(Pin lightPin, SSD1306 display, long Cooldown, long RollingAvg){
        if (lightPin.getValue() == 0){
            display.clear();
            System.out.println(RollingAvg + "% Dry");
            display.getCanvas().drawString(0, 0, (RollingAvg)+"% Dry");
            display.getCanvas().drawRect(0, 16, 128, 4, MonochromeCanvas.Color.BRIGHT);
            if(Cooldown > 0) {
                display.getCanvas().drawString(0,22,"Saturating");
            }else{
                display.getCanvas().drawString(0,22,"Ready to\nwater :D");
            }
            display.display();
            Cooldown--;
        }
        return(Cooldown);
    }


    @Override
    public void run() {
        try {
            if(Running){
                long averageSize = 5;
                wetValue = PlantsLogic.ValuesArray(wetValue, averageSize, sensorPin);
                int minWetReading = 512;
                int maxWetReading = 728;
                long rollingAvg = PlantsLogic.RollingAverage(wetValue, minWetReading, maxWetReading);
                long dryThreshold = 80;
                long cooldownConstant = 5;
                cooldown = PlantsLogic.Watering(display, MOSFETPin, rollingAvg, dryThreshold, cooldown, cooldownConstant);
                cooldown = PlantsLogic.Update(lightPin, display, cooldown, rollingAvg);
                System.out.println(cooldown);
                fileWriter.write(rollingAvg+",");
                }
        } catch (Exception e) {
            System.out.println("Error in timer: " + e.getMessage());
        }
    }
    @Override
    public void onStart(IOEvent ioEvent) {
    }
    @Override
    public void onStop(IOEvent ioEvent) {
    }
    @Override
    public void onPinChange(IOEvent ioEvent) {
        if (ioEvent.getPin().getIndex() != buttonPin.getIndex()) return;
        if (!Running) return;
        try {
            if (buttonPin.getValue() == 1) {
                Running = false;
                display.clear();
                Thread.sleep(500);
                System.out.println("Force Stop");
                fileWriter.close();
                MOSFETPin.setValue(0);
                lightPin.setValue(0);
                System.exit(100);

            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error");
        }
    }    @Override
    public void onMessageReceive(IOEvent ioEvent, String s) {
    }
}

