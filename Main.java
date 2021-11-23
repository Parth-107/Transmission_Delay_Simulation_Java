
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class Main extends JFrame 
{

    public static void main(String[] args) {
        Main simulator = new Main();
        simulator.initial();

    }
    //buttons
    Button start_button;
    //set the length of link, packet size and rate of the packet
    CustomChoice link_length = new CustomChoice(new String[]{"10 km", "100 km","500 km", "1000 km"}, new double[]{10E3, 100E3, 500E3,1E6}, 1);
    CustomChoice rate = new CustomChoice(new String[]{"512 kps", "1 Mbps", "10 Mbps", "100 Mbps"}, new double[]{512E3, 1E6, 10E6, 100E6}, 1);
    CustomChoice packet_size = new CustomChoice(new String[]{"100 Bytes", "500 Bytes", "1 kBytes"}, new double[]{8E2, 4E3, 8E3}, 1);
    
    //Simulate time
    Thread threadTime;
    TaskTimer taskTime;
    boolean isRunning = false;
    Progress_Bar progressBar;

    public void initial() {
        try {
            Button reset_button = new Button("Reset");//Reset everything
            reset_button.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    stop_simulation();
                    //clear line
                    progressBar.sendTime(0);
                    Main.this.repaint();
                    }
                });
            add(reset_button);
            progressBar = new Progress_Bar(80, 160, 500, 15);//(X, Y, Width, Height)

            setBackground(Color.WHITE);
            final TextField tfTPD = new TextField();
            tfTPD.setBounds(50, 50, 150, 20);
            start_button = new Button("Start");
            start_button.setBounds(500, 80, 60, 30);
            //If user click on 
            reset_button.setBounds(580, 80, 60, 30);
            //Start the action
            start_button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    start_simulation();
                }
            });
            Label label_link_length = new Label("Length");
            label_link_length.setBounds(30, 80, 40, 30);
            label_link_length.setBackground(Color.WHITE);
            add(label_link_length);
            link_length.setBounds(80, 83, 70, 30);
            add(link_length);

            Label TPDlabel_rate= new Label("Rate");
            TPDlabel_rate.setBounds(183, 80, 40, 30);
            TPDlabel_rate.setBackground(Color.WHITE);
            add(TPDlabel_rate);
            rate.setBounds(230, 83, 80, 30);
            add(rate);

            Label label_ps = new Label("Packet Size");
            label_ps.setBounds(330, 80, 70, 30);
            label_ps.setBackground(Color.WHITE);
            add(label_ps);
            packet_size.setBounds(405, 83, 75, 30);
            add(packet_size);
            add(start_button);
            
            setSize(700, 400);//set Jframe size 
            setLayout(null);
            setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void paint(Graphics g) {
        graphics_update(g);
    }

    public void graphics_update(Graphics graphics) { //work on a offscreen image

        Dimension dimension = getSize();
        Image img = createImage(dimension.width, dimension.height);
        Graphics graphic_img = img.getGraphics();
        progressBar.draw_line(graphic_img);

        //sender
       
        Toolkit t=Toolkit.getDefaultToolkit();
        Image i=t.getImage("E:\\Netbeans\\Java_Projects\\src\\main\\java\\com\\mycompany\\java_projects\\SR.png");
        //SenderImg
        graphic_img.setColor(Color.blue);
        graphic_img.drawImage(i, 30, 150, 30, 30,this);
        //receiverImg
        graphic_img.setColor(Color.blue);
        graphic_img.drawImage(i, 595,150, 30, 30,this);

        graphic_img.setColor(Color.black);
        graphic_img.drawString("Sender", 50, 200);

        graphic_img.setColor(Color.black);
        graphic_img.drawString("Receiver", 550, 200);

        graphic_img.drawString("Propagation speed : 2.8 x 10^8 m/sec", 240, 226);
        //display offscreen image
        graphics.drawImage(img, 0, 0, this);
    }

    private void start_simulation() {
        setupEnabled(false);
        progressBar.setup(link_length.getVal(), rate.getVal());
        progressBar.emitPacket(packet_size.getVal(), 0);
        //setup timer
        taskTime = new TaskTimer(1E-5, progressBar.totalTime());
        threadTime = new Thread(taskTime);
        //start simulation
        isRunning = true;
        threadTime.start();
    }

    private void stop_simulation() {
        taskTime.endNow();
        isRunning = false;
        setupEnabled(true);
    }

    public void setupEnabled(boolean value) {
        start_button.setEnabled(value);
        link_length.setEnabled(value);
        rate.setEnabled(value);
        packet_size.setEnabled(value);
    }

    //my choice
    class CustomChoice extends Choice {
        private double vals[];

        public CustomChoice(String items[], double values[], int defaultValue) {
            for (int i = 0; i < items.length; i++) {
                super.addItem(items[i]);
            }
            vals = values;
            select(defaultValue - 1);
        }

        public double getVal() {
            return vals[super.getSelectedIndex()];
        }
    }

    //tickTask
    class TaskTimer implements Runnable 
    {
        private double counter;
        private double length;
        private double tick;

        public TaskTimer(double t, double l) {
            length = l;
            tick = t;
            counter = 0;
        }

        public void run() {
            while (Main.this.isRunning) {
                counter += tick;
                Main.this.progressBar.sendTime(counter);
                Main.this.repaint();
                if (counter >= length) {
                    Main.this.progressBar.clearPacketsTPD();
                    Main.this.threadTime.checkAccess();
                }
                try {
                    Main.this.threadTime.sleep(50);
                } catch (Exception e) {
                }
            }
        }

        public void endNow() {
            length = counter;
        }
    }
}//End of Main class

//Line class
class Progress_Bar 
{
    //graphic variables
    private int graphic_X;
    private int graphic_Y;
    private int graphic_width;
    private int graphic_height;
    //characteristic variables
    final double celerity = 2.8E+8;
    private double link_length;
    private double rate;
    //simulation variables
    private double timeTPD;
    private DataPackets packet;

    public Progress_Bar(int x, int y, int w, int h) {
        //graphic init
        graphic_X = x;
        graphic_Y = y;
        graphic_width = w;
        graphic_height = h;
    }

    public void setup(double l, double r) {
        link_length = l;
        rate = r;
    }

    void sendTime(double now) {
        timeTPD = now; //graphics_update time
        removeReceivedPackets(now);
    }

    void emitPacket(double s, double eT) {
        packet = new DataPackets(s, eT);
    }

    private void removeReceivedPackets(double now) {
        if (!(packet == null)) {
            if (now > packet.emissionTimeTPD + (packet.packet_size / rate) + link_length * celerity) {
                clearPacketsTPD();
            }
        }
    }

    public void clearPacketsTPD() {
        packet = null;
    }

    public double totalTime() {
        double emmissionTime = (packet.packet_size / rate);
        double onLineTime = (link_length / celerity);
        System.out.println(emmissionTime + onLineTime);
        return (emmissionTime + onLineTime);
    }

    static String last = "";

    public void draw_line(Graphics g) {

        g.setColor(Color.white);
        g.fillRect(graphic_X, graphic_Y + 1, graphic_width, graphic_height - 2);
        g.setColor(Color.black);
        g.drawRect(graphic_X, graphic_Y, graphic_width, graphic_height);
        g.setColor(Color.red);
        if (!(packet == null)) {
            last = timeToString(timeTPD);
            g.drawString(last, graphic_X + graphic_width / 2 - 10, graphic_Y + graphic_height + 15);
        } else {

            if (timeTPD == 0) {
                last = timeToString(0);
            }
            g.drawString(last, graphic_X + graphic_width / 2 - 10, graphic_Y + graphic_height + 15);
        }
        drawPackets(g);
    }

    private void drawPackets(Graphics g) {
        if (!(packet == null)) {
            double xfirst;
            double xlast;
            //compute time units
            xfirst = timeTPD - packet.emissionTimeTPD;
            xlast = xfirst - (packet.packet_size / rate);
            //compute position
            xfirst = xfirst * celerity * graphic_width / link_length;
            xlast = xlast * celerity * graphic_width / link_length;
            if (xlast < 0) {
                xlast = 0;

            }
            if (xfirst > graphic_width) {
                xfirst = graphic_width;
            }
            //draw
            g.setColor(Color.red);
            g.fillRect(graphic_X + (int) (xlast), graphic_Y + 1, (int) (xfirst - xlast), graphic_height - 2);
        }
    }

    static private String timeToString(double now) {
        String res = Double.toString(now * 1000);
        int dot = res.indexOf('.');
        String deci = res.substring(dot + 1) + "000";
        deci = deci.substring(0, 3);
        String inte = res.substring(0, dot);
        return inte + "." + deci + " ms";
    }
}//End of line class

class DataPackets 
{
    double packet_size;
    double emissionTimeTPD;

    DataPackets(double s, double eT) {
        packet_size = s;
        emissionTimeTPD = eT;
    }
}
