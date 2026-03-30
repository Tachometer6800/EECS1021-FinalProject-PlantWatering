import EECS1021.FinalProject.PlantsLogic;
import org.firmata4j.I2CDevice;
import org.firmata4j.Pin;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.ssd1306.SSD1306;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

static final int A1 = 15; // Moisture Sensor
static final int D2 = 2; // Reset Button
static final int D4 = 4; // LED
static final byte DISPLAY = 0x3C; // OLED Display
static final int D7 = 7; // MOSFET
int timerLength = 5000; // Change to set how often timer goes off
private static final String FOLDER_PATH = "C:/User/GenericUserPath/"; // Change to path you want CSV files to save to

private static String generateFilePath(){
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
    String timestamp = LocalDateTime.now().format(formatter);
    return(FOLDER_PATH + "data_" + timestamp + ".csv");
};

void main()
    throws InterruptedException, IOException {

    File CSVoutput = new File(generateFilePath());
    CSVoutput.getParentFile().mkdirs();
    FileWriter fileWriter = new FileWriter(CSVoutput);

    // Initialize Arduino
    var device = new FirmataDevice("COM3");
    device.start();
    device.ensureInitializationIsDone();

    // Initialize Display
    I2CDevice i2cObject = device.getI2CDevice(DISPLAY);
    SSD1306 disp = new SSD1306(i2cObject, SSD1306.Size.SSD1306_128_64);
    PlantsLogic.initialize(disp);

    // Initialize MOSFET & Pump
    var MOSFET = device.getPin(D7);
    MOSFET.setMode(Pin.Mode.OUTPUT);

    // Initialize LED
    var LED = device.getPin(D4);
    LED.setMode(Pin.Mode.OUTPUT);

    // Initialize Moisture Sensor
    var WetTester = device.getPin(A1);
    WetTester.setMode(Pin.Mode.ANALOG);

    // Initialize Reset Button
    var RESET_BUTTON = device.getPin(D2);
    RESET_BUTTON.setMode(Pin.Mode.INPUT);

    PlantsLogic EventListener = new PlantsLogic(RESET_BUTTON, MOSFET, LED, WetTester, disp, fileWriter);
    device.addEventListener(EventListener);

    Timer timer = new Timer();
    timer.schedule(EventListener, 0, timerLength);
}
