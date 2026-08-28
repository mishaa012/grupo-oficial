import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class HospitalProApp {

    static final Color BG = new Color(8, 10, 14);
    static final Color SIDEBAR = new Color(13, 16, 22);
    static final Color PANEL = new Color(19, 23, 31);
    static final Color PANEL2 = new Color(29, 34, 44);
    static final Color TEXT = new Color(238, 242, 248);
    static final Color MUTED = new Color(155, 166, 182);
    static final Color PRIMARY = new Color(65, 135, 255);
    static final Color GREEN = new Color(59, 185, 112);
    static final Color RED = new Color(225, 83, 98);
    static final Color YELLOW = new Color(230, 188, 58);
    static final Color ORANGE = new Color(236, 139, 56);
    static final Color GRAY = new Color(104, 111, 125);
    static final Color CYAN = new Color(49, 196, 212);
    static final Color PURPLE = new Color(150, 106, 255);
    static final Color BORDER = new Color(55, 64, 78);
    static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ===================== MODELOS =====================
    static class User implements Serializable {
        String username, passwordHash, name, role;
        User(String username, String password, String name, String role) {
            this.username = username;
            this.passwordHash = sha256(password);
            this.name = name;
            this.role = role;
        }
    }

    static class Patient implements Serializable {
        int id, age;
        String document, fullName, sex, phone;
        LocalDateTime createdAt = LocalDateTime.now();

        Patient(int id, String document, String fullName, int age, String sex, String phone) {
            this.id = id;
            this.document = document;
            this.fullName = fullName;
            this.age = age;
            this.sex = sex;
            this.phone = phone;
        }
        public String toString() { return fullName + " - CI: " + document; }
    }

    static class Bed implements Serializable {
        int id;
        String code, area, status, observation;
        Integer patientId;
        LocalDateTime updatedAt = LocalDateTime.now();

        Bed(int id, String code, String area, String status) {
            this.id = id;
            this.code = code;
            this.area = area;
            this.status = status;
            this.observation = "";
        }
        public String toString() { return code + " - " + area; }
    }

    static class Triage implements Serializable {
        int id, patientId, heartRate;
        String bloodPressure, level, colorName, category, attentionTime, notes;
        double temperature;
        LocalDateTime createdAt = LocalDateTime.now();

        Triage(int id, int patientId, String bp, double temp, int hr, String level,
               String color, String category, String attention, String notes) {
            this.id = id;
            this.patientId = patientId;
            this.bloodPressure = bp;
            this.temperature = temp;
            this.heartRate = hr;
            this.level = level;
            this.colorName = color;
            this.category = category;
            this.attentionTime = attention;
            this.notes = notes;
        }
    }

    static class Audit implements Serializable {
        int id;
        String username, action, detail;
        LocalDateTime createdAt = LocalDateTime.now();
        Audit(int id, String username, String action, String detail) {
            this.id = id; this.username = username; this.action = action; this.detail = detail;
        }
    }

    static class State implements Serializable {
        List<Patient> patients = new ArrayList<>();
        List<Bed> beds = new ArrayList<>();
        List<Triage> triages = new ArrayList<>();
        List<Audit> audits = new ArrayList<>();
    }

    // ===================== DATOS =====================
    static class Store {
        static final Path DIR = Paths.get(System.getProperty("user.home"), "SistemaHospitalCamillasJavaPro");
        static final Path FILE = DIR.resolve("hospital_pro.bin");
        static State state;

        static void init() {
            try {
                Files.createDirectories(DIR);
                if (Files.exists(FILE)) {
                    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(FILE))) {
                        state = (State) in.readObject();
                    }
                } else {
                    state = seed();
                    save();
                }
            } catch (Exception ex) {
                state = seed();
                save();
            }
        }

        static State seed() {
            State s = new State();
            s.patients.add(new Patient(1, "10245", "Juan Pérez", 45, "Masculino", "70701010"));
            s.patients.add(new Patient(2, "20880", "Ana López", 32, "Femenino", "60702020"));
            s.patients.add(new Patient(3, "30991", "María Flores", 28, "Femenino", "71703030"));

            Bed a = new Bed(1, "CAMILLA 001", "Área de emergencia", "Libre");
            Bed b = new Bed(2, "CAMILLA 002", "Box 1", "Ocupada"); b.patientId = 2;
            Bed c = new Bed(3, "CAMILLA 003", "Pasillo Norte", "En limpieza");
            Bed d = new Bed(4, "CAMILLA 004", "Sala de observación", "Falla"); d.observation = "Rueda con desperfecto";
            Bed e = new Bed(5, "CAMILLA 005", "Box 2", "Libre");
            Bed f = new Bed(6, "CAMILLA 006", "Observación 2", "Libre");
            s.beds.addAll(Arrays.asList(a,b,c,d,e,f));
            return s;
        }

        static void save() {
            try {
                Files.createDirectories(DIR);
                try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(FILE))) {
                    out.writeObject(state);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error guardando datos:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        static int nextPatientId() { return state.patients.stream().mapToInt(x -> x.id).max().orElse(0) + 1; }
        static int nextBedId() { return state.beds.stream().mapToInt(x -> x.id).max().orElse(0) + 1; }
        static int nextTriageId() { return state.triages.stream().mapToInt(x -> x.id).max().orElse(0) + 1; }
        static int nextAuditId() { return state.audits.stream().mapToInt(x -> x.id).max().orElse(0) + 1; }

        static void audit(String action, String detail) {
            String user = Session.user == null ? "sistema" : Session.user.username;
            state.audits.add(new Audit(nextAuditId(), user, action, detail));
            save();
        }

        static void backup(File destination) throws IOException {
            save();
            Files.copy(FILE, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        static void restore(File source) throws Exception {
            Files.copy(source.toPath(), FILE, StandardCopyOption.REPLACE_EXISTING);
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(FILE))) {
                state = (State) in.readObject();
            }
        }
    }

    // ===================== SEGURIDAD =====================
    static class Session {
        static User user;
        static boolean admin() { return user != null && "Administrador".equals(user.role); }
        static boolean medical() { return user != null && "Medico".equals(user.role); }
        static boolean maintenance() { return user != null && "Mantenimiento".equals(user.role); }

        static boolean managePatients() { return admin() || medical(); }
        static boolean manageBeds() { return admin(); }
        static boolean assign() { return admin() || medical(); }
        static boolean changeStatus() { return admin() || maintenance(); }
        static boolean triage() { return admin() || medical(); }
        static boolean reports() { return admin() || medical(); }
        static boolean tools() { return admin(); }
    }

    static class Auth {
        static final List<User> USERS = Arrays.asList(
                new User("admin","1234","Administrador del sistema","Administrador"),
                new User("medico","1234","Personal médico","Medico"),
                new User("mantenimiento","1234","Personal de mantenimiento","Mantenimiento"),
                new User("consulta","1234","Usuario de consulta","Consulta")
        );
        static User login(String u, String p) {
            String hash = sha256(p);
            return USERS.stream()
                    .filter(x -> x.username.equalsIgnoreCase(u.trim()) && x.passwordHash.equals(hash))
                    .findFirst().orElse(null);
        }
    }

    public static void main(String[] args) {
        Store.init();
        if (args.length > 0 && "--verificar".equalsIgnoreCase(args[0])) {
            System.out.println("Sistema Hospitalario PRO V3 verificado. Camillas: " + Store.state.beds.size());
            return;
        }
        SwingUtilities.invokeLater(() -> {
            darkDefaults();
            new LoginFrame().setVisible(true);
        });
    }

    // ===================== LOGIN =====================
    static class LoginFrame extends JFrame {
        JTextField user = new JTextField("admin");
        JPasswordField pass = new JPasswordField("1234");

        LoginFrame() {
            setTitle("Sistema Hospitalario PRO V3 - Inicio de sesión");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(540, 570);
            setResizable(false);
            setLocationRelativeTo(null);
            getContentPane().setBackground(BG);
            setLayout(new GridBagLayout());

            JPanel card = panel();
            card.setPreferredSize(new Dimension(420, 440));
            card.setLayout(new GridBagLayout());
            card.setBorder(new CompoundBorder(new LineBorder(new Color(58,65,78)),
                    new EmptyBorder(28,38,28,38)));
            GridBagConstraints g = gbc();

            JLabel title = new JLabel("HOSPITAL PRO  |  V3", SwingConstants.CENTER);
            title.setForeground(PRIMARY);
            title.setFont(new Font("Segoe UI",Font.BOLD,28));
            g.gridx=0; g.gridy=0; g.fill=GridBagConstraints.HORIZONTAL;
            card.add(title,g);

            JLabel sub = new JLabel("Camillas · Pacientes · Triaje", SwingConstants.CENTER);
            sub.setForeground(MUTED);
            g.gridy=1; g.insets=new Insets(5,5,24,5);
            card.add(sub,g);

            g.gridy=2; g.insets=new Insets(6,5,5,5);
            card.add(label("Usuario"),g);
            styleField(user); g.gridy=3;
            card.add(user,g);

            g.gridy=4; g.insets=new Insets(15,5,5,5);
            card.add(label("Contraseña"),g);
            styleField(pass); g.gridy=5; g.insets=new Insets(5,5,20,5);
            card.add(pass,g);

            JButton btn = primary("INGRESAR");
            g.gridy=6; card.add(btn,g);

            JLabel hint = new JLabel("Accesos de demostración", SwingConstants.CENTER);
            hint.setForeground(MUTED);
            g.gridy=7; g.insets=new Insets(10,5,5,5);
            card.add(hint,g);

            JPanel demos = new JPanel(new GridLayout(2,2,7,7));
            demos.setOpaque(false);
            JButton adminDemo=secondary("Administrador");
            JButton medicalDemo=secondary("Médico");
            JButton maintenanceDemo=secondary("Mantenimiento");
            JButton consultDemo=secondary("Solo consulta");
            adminDemo.addActionListener(e->loginAs("admin"));
            medicalDemo.addActionListener(e->loginAs("medico"));
            maintenanceDemo.addActionListener(e->loginAs("mantenimiento"));
            consultDemo.addActionListener(e->loginAs("consulta"));
            demos.add(adminDemo); demos.add(medicalDemo); demos.add(maintenanceDemo); demos.add(consultDemo);
            g.gridy=8; g.insets=new Insets(8,5,5,5);
            card.add(demos,g);

            btn.addActionListener(e -> login());
            getRootPane().setDefaultButton(btn);
            add(card);
        }

        void loginAs(String username) {
            user.setText(username);
            pass.setText("1234");
            login();
        }

        void login() {
            if (user.getText().isBlank() || pass.getPassword().length == 0) {
                warn("Ingrese usuario y contraseña."); return;
            }
            User u = Auth.login(user.getText(), new String(pass.getPassword()));
            if (u == null) { warn("Usuario o contraseña incorrectos."); return; }
            Session.user = u;
            Store.audit("Inicio de sesión","Ingresó como " + u.role);
            dispose();
            new MainFrame().setVisible(true);
        }
    }

    // ===================== MAIN =====================
    static class MainFrame extends JFrame {
        JPanel content = new JPanel(new BorderLayout());
        JLabel section = new JLabel("Inicio");

        MainFrame() {
            setTitle("Sistema Hospitalario PRO V3 - Gestión de camillas");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setMinimumSize(new Dimension(1200,740));
            setExtendedState(MAXIMIZED_BOTH);

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(BG);
            setContentPane(root);

            JPanel side = new JPanel();
            side.setBackground(SIDEBAR);
            side.setPreferredSize(new Dimension(245,0));
            side.setLayout(new BoxLayout(side,BoxLayout.Y_AXIS));

            JLabel logo = new JLabel("<html><center>HOSPITAL<br><span style='color:#4187ff'>PRO V3</span><br><font size='2'>GESTIÓN DE CAMILLAS</font></center></html>",SwingConstants.CENTER);
            logo.setForeground(TEXT);
            logo.setFont(new Font("Segoe UI",Font.BOLD,22));
            logo.setMaximumSize(new Dimension(Integer.MAX_VALUE,118));
            side.add(logo);

            nav(side,"⌂  Inicio",this::dashboard);
            nav(side,"▦  Mapa de camillas",() -> showBedMap("Todos"));
            nav(side,"▤  Estado de camillas",() -> show(new BedsPanel(),"Estado de camillas"));
            if(Session.managePatients()) nav(side,"♙  Pacientes",() -> show(new PatientsPanel(),"Pacientes"));
            if(Session.assign()) nav(side,"⇄  Asignar / liberar",() -> show(new AssignPanel(),"Asignar / liberar camilla"));
            if(Session.changeStatus()) nav(side,"⚙  Limpieza / falla",() -> show(new StatusPanel(),"Limpieza / Falla"));
            if(Session.triage()) nav(side,"✚  Triaje",() -> show(new TriagePanel(),"Triaje"));
            if(Session.reports()) nav(side,"▥  Reportes",() -> show(new ReportsPanel(),"Reportes"));
            if(Session.tools()) nav(side,"⚒  Herramientas",() -> show(new ToolsPanel(),"Herramientas"));

            side.add(Box.createVerticalGlue());
            nav(side,"↩  Cerrar sesión",this::logout);

            JPanel top = panel();
            top.setPreferredSize(new Dimension(0,72));
            top.setLayout(new BorderLayout());
            top.setBorder(new EmptyBorder(0,24,0,24));
            section.setForeground(TEXT);
            section.setFont(new Font("Segoe UI",Font.BOLD,19));
            top.add(section,BorderLayout.WEST);

            JLabel who = new JLabel(Session.user.name + "  |  " + Session.user.role);
            who.setForeground(MUTED);
            JLabel online = new JLabel("● Sistema operativo");
            online.setForeground(GREEN);
            JButton quick = primary("Acciones rápidas");
            quick.addActionListener(e->openQuickActions());
            JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,14));
            topRight.setOpaque(false);
            topRight.add(online); topRight.add(who); topRight.add(quick);
            top.add(topRight,BorderLayout.EAST);

            content.setBackground(BG);
            content.setBorder(new EmptyBorder(18,18,18,18));

            root.add(side,BorderLayout.WEST);
            root.add(top,BorderLayout.NORTH);
            root.add(content,BorderLayout.CENTER);

            dashboard();
        }

        void nav(JPanel side,String text,Runnable action) {
            JButton b=new JButton(text);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE,48));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setBorder(new EmptyBorder(0,18,0,4));
            b.setBackground(SIDEBAR);
            b.setForeground(TEXT);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setFont(new Font("Segoe UI",Font.PLAIN,14));
            b.addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent e){ b.setBackground(PANEL2); }
                public void mouseExited(MouseEvent e){ b.setBackground(SIDEBAR); }
            });
            b.addActionListener(e->action.run());
            side.add(b);
        }

        void show(JPanel p,String title) {
            section.setText(title);
            content.removeAll();
            content.add(p,BorderLayout.CENTER);
            content.revalidate();
            content.repaint();
        }

        void dashboard(){ show(new DashboardPanel(this),"Inicio"); }

        void showBedMap(String filter){
            show(new BedMapPanel(this,filter),"Mapa interactivo de camillas");
        }

        void openQuickActions(){
            JDialog dialog=new JDialog(this,"Acciones rápidas",false);
            dialog.setLayout(new BorderLayout(0,12));
            dialog.getContentPane().setBackground(BG);
            JPanel header=panel();header.setBorder(new EmptyBorder(16,18,12,18));
            header.setLayout(new BoxLayout(header,BoxLayout.Y_AXIS));
            JLabel t=subtitle("Acciones disponibles para " + Session.user.role);t.setAlignmentX(LEFT_ALIGNMENT);
            JLabel d=new JLabel("Accede directamente al módulo que necesitas.");d.setForeground(MUTED);d.setAlignmentX(LEFT_ALIGNMENT);
            header.add(t);header.add(Box.createVerticalStrut(4));header.add(d);
            dialog.add(header,BorderLayout.NORTH);

            JPanel grid=new JPanel(new GridLayout(0,2,12,12));grid.setBackground(BG);grid.setBorder(new EmptyBorder(0,18,18,18));
            quickAction(grid,"Abrir mapa","Revisa en tiempo real el estado de cada camilla.",()->{dialog.dispose();showBedMap("Todos");});
            quickAction(grid,"Ver camillas libres","Encuentra una camilla disponible rápidamente.",()->{dialog.dispose();show(new BedsPanel("Libre"),"Camillas libres");});
            if(Session.assign()) quickAction(grid,"Asignar paciente","Vincula un paciente con una camilla libre.",()->{dialog.dispose();show(new AssignPanel(),"Asignar / liberar camilla");});
            if(Session.managePatients()) quickAction(grid,"Registrar paciente","Crea o busca el registro del paciente.",()->{dialog.dispose();show(new PatientsPanel(),"Pacientes");});
            if(Session.changeStatus()) quickAction(grid,"Limpieza o falla","Actualiza disponibilidad y observaciones.",()->{dialog.dispose();show(new StatusPanel(),"Limpieza / Falla");});
            if(Session.triage()) quickAction(grid,"Registrar triaje","Ingresa una atención de emergencia.",()->{dialog.dispose();show(new TriagePanel(),"Triaje");});
            dialog.add(grid,BorderLayout.CENTER);
            dialog.setSize(680,390);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }

        void quickAction(JPanel grid,String name,String detail,Runnable action){
            JPanel p=panel();p.setLayout(new BorderLayout(0,9));p.setBorder(new CompoundBorder(new MatteBorder(0,4,0,0,PRIMARY),new EmptyBorder(12,13,12,13)));
            JLabel n=subtitle(name);JLabel d=new JLabel("<html>"+detail+"</html>");d.setForeground(MUTED);
            JButton go=primary("Abrir");go.addActionListener(e->action.run());
            p.add(n,BorderLayout.NORTH);p.add(d,BorderLayout.CENTER);p.add(go,BorderLayout.SOUTH);grid.add(p);
        }

        void logout() {
            if(JOptionPane.showConfirmDialog(this,"¿Cerrar sesión?","Confirmar",
                    JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
                Store.audit("Cierre de sesión","Usuario cerró la sesión");
                Session.user=null;
                dispose();
                new LoginFrame().setVisible(true);
            }
        }
    }

    // ===================== DASHBOARD =====================
    static class DashboardPanel extends JPanel {
        MainFrame host;

        DashboardPanel(MainFrame host) {
            this.host=host;
            setBackground(BG); setLayout(new BorderLayout(0,14));

            JPanel north=new JPanel();
            north.setOpaque(false);
            north.setLayout(new BoxLayout(north,BoxLayout.Y_AXIS));
            JLabel t=title("Centro de control"); t.setAlignmentX(LEFT_ALIGNMENT); north.add(t);
            JLabel sub=new JLabel("Consulta el estado del hospital y usa los accesos rápidos para trabajar.");
            sub.setForeground(MUTED); sub.setAlignmentX(LEFT_ALIGNMENT); north.add(sub);
            north.add(Box.createVerticalStrut(14));

            JPanel cards=new JPanel(new GridLayout(2,3,10,10)); cards.setOpaque(false);
            cards.add(metricCard("Camillas libres",countBeds("Libre"),GREEN,()->host.show(new BedsPanel("Libre"),"Camillas libres")));
            cards.add(metricCard("Camillas ocupadas",countBeds("Ocupada"),RED,()->host.showBedMap("Ocupada")));
            cards.add(metricCard("En limpieza",countBeds("En limpieza"),YELLOW,()->host.showBedMap("En limpieza")));
            cards.add(metricCard("Con falla",countBeds("Falla"),GRAY,()->host.showBedMap("Falla")));
            cards.add(metricCard("Pacientes",Store.state.patients.size(),PRIMARY,()->{
                if(Session.managePatients()) host.show(new PatientsPanel(),"Pacientes"); else warn("Tu usuario es de solo consulta para pacientes.");
            }));
            cards.add(metricCard("Triajes hoy",Store.state.triages.size(),ORANGE,()->{
                if(Session.triage()) host.show(new TriagePanel(),"Triaje"); else warn("Tu usuario no tiene permiso para registrar triaje.");
            }));
            north.add(cards);

            JPanel overview=new JPanel(new GridLayout(1,2,14,0));overview.setOpaque(false);
            overview.add(controlCard());
            overview.add(mapPreview());

            JPanel activity=panel(); activity.setLayout(new BorderLayout(0,8));
            activity.setBorder(new EmptyBorder(13,14,14,14));
            activity.add(subtitle("Actividad reciente"),BorderLayout.NORTH);
            DefaultTableModel m=ro("Fecha","Usuario","Acción","Detalle");
            Store.state.audits.stream().sorted(Comparator.comparing((Audit a)->a.createdAt).reversed()).limit(18)
                    .forEach(a->m.addRow(new Object[]{a.createdAt.format(DF),a.username,a.action,a.detail}));
            activity.add(scroll(table(m)),BorderLayout.CENTER);

            JPanel center=new JPanel(new BorderLayout(0,14));center.setOpaque(false);
            center.add(overview,BorderLayout.NORTH);center.add(activity,BorderLayout.CENTER);
            add(north,BorderLayout.NORTH); add(center,BorderLayout.CENTER);
        }

        JButton metricCard(String name,int value,Color accent,Runnable action){
            JButton b=new JButton("<html><b><font size='6'>"+value+"</font></b><br>"+name+"<br><font color='#aeb9cb'>Ver detalle →</font></html>");
            b.setHorizontalAlignment(SwingConstants.LEFT);b.setVerticalAlignment(SwingConstants.CENTER);
            b.setBackground(PANEL);b.setForeground(TEXT);b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setBorder(new CompoundBorder(new MatteBorder(0,5,0,0,accent),new EmptyBorder(9,13,9,13)));
            b.addActionListener(e->action.run());
            return b;
        }

        JPanel controlCard(){
            JPanel p=panel();p.setLayout(new BorderLayout(0,10));p.setBorder(new EmptyBorder(14,15,14,15));
            p.add(subtitle("Acciones y capacidad"),BorderLayout.NORTH);
            JPanel body=new JPanel();body.setOpaque(false);body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));
            int total=Math.max(Store.state.beds.size(),1),occupied=countBeds("Ocupada");
            JLabel capacity=new JLabel("Ocupación actual: "+occupied+" de "+Store.state.beds.size()+" camillas");capacity.setForeground(MUTED);
            JProgressBar progress=new JProgressBar(0,100);progress.setValue(occupied*100/total);progress.setStringPainted(true);progress.setString(progress.getValue()+"% ocupada");
            progress.setForeground(occupied*100/total>=80?RED:PRIMARY);progress.setBackground(PANEL2);progress.setBorder(new LineBorder(BORDER));
            body.add(capacity);body.add(Box.createVerticalStrut(7));body.add(progress);body.add(Box.createVerticalStrut(13));
            JPanel actions=new JPanel(new GridLayout(0,2,8,8));actions.setOpaque(false);
            actions.add(shortcut("Mapa completo",true,()->host.showBedMap("Todos")));
            actions.add(shortcut("Camillas libres",true,()->host.show(new BedsPanel("Libre"),"Camillas libres")));
            actions.add(shortcut("Asignar paciente",Session.assign(),()->host.show(new AssignPanel(),"Asignar / liberar camilla")));
            actions.add(shortcut("Registrar paciente",Session.managePatients(),()->host.show(new PatientsPanel(),"Pacientes")));
            actions.add(shortcut("Limpieza / falla",Session.changeStatus(),()->host.show(new StatusPanel(),"Limpieza / Falla")));
            actions.add(shortcut("Registrar triaje",Session.triage(),()->host.show(new TriagePanel(),"Triaje")));
            body.add(actions);p.add(body,BorderLayout.CENTER);return p;
        }

        JButton shortcut(String text,boolean enabled,Runnable action){
            JButton b=secondary(text);b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setEnabled(enabled);b.setToolTipText(enabled?text:"No tienes permiso para esta acción.");
            if(enabled)b.addActionListener(e->action.run());
            return b;
        }

        JPanel mapPreview(){
            JPanel p=panel();p.setLayout(new BorderLayout(0,10));p.setBorder(new EmptyBorder(14,15,14,15));
            JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);header.add(subtitle("Mapa rápido de camillas"),BorderLayout.WEST);
            JButton open=secondary("Abrir mapa");open.addActionListener(e->host.showBedMap("Todos"));header.add(open,BorderLayout.EAST);p.add(header,BorderLayout.NORTH);
            JPanel grid=new JPanel(new GridLayout(0,2,8,8));grid.setOpaque(false);
            Store.state.beds.stream().sorted(Comparator.comparing(b->b.code)).limit(6).forEach(b->grid.add(previewBed(b)));
            p.add(grid,BorderLayout.CENTER);return p;
        }

        JButton previewBed(Bed b){
            Patient p=b.patientId==null?null:findPatient(b.patientId);
            JButton card=new JButton("<html><b>"+b.code+"</b><br>"+b.status+"<br><font size='2'>"+(p==null?b.area:p.fullName)+"</font></html>");
            card.setHorizontalAlignment(SwingConstants.LEFT);card.setBackground(bedColor(b.status));card.setForeground(contrast(bedColor(b.status)));
            card.setFocusPainted(false);card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));card.setBorder(new EmptyBorder(7,9,7,9));
            card.addActionListener(e->host.show(new BedMapPanel(host,"Todos",b.id),"Mapa interactivo de camillas"));
            return card;
        }
    }

    // ===================== MAPA INTERACTIVO =====================
    static class BedMapPanel extends JPanel {
        MainFrame host;
        JTextField search=new JTextField();
        JComboBox<String> filter=new JComboBox<>(new String[]{"Todos","Libre","Ocupada","En limpieza","Falla"});
        JPanel cards=new JPanel();
        JLabel summary=new JLabel();
        JLabel selectedTitle=new JLabel("Sin camilla seleccionada",SwingConstants.CENTER);
        JTextArea selectedInfo=infoArea();
        JButton action=primary("Acción según estado");
        Bed selected;
        Integer initialSelectedId;

        BedMapPanel(MainFrame host,String initialFilter){this(host,initialFilter,null);}

        BedMapPanel(MainFrame host,String initialFilter,Integer initialSelectedId){
            this.host=host;this.initialSelectedId=initialSelectedId;
            setBackground(BG);setLayout(new BorderLayout(0,10));
            JPanel header=new JPanel();header.setOpaque(false);header.setLayout(new BoxLayout(header,BoxLayout.Y_AXIS));
            JLabel t=title("Mapa interactivo de camillas");t.setAlignmentX(LEFT_ALIGNMENT);header.add(t);
            JLabel sub=new JLabel("Haz clic en una camilla para ver detalles y realizar la acción disponible.");sub.setForeground(MUTED);sub.setAlignmentX(LEFT_ALIGNMENT);header.add(sub);
            JPanel tools=new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));tools.setOpaque(false);
            styleField(search);search.setPreferredSize(new Dimension(190,32));styleCombo(filter);
            JButton reset=secondary("Restablecer");JButton refresh=primary("Actualizar");JButton details=secondary("Ver detalle");
            tools.add(label("Buscar:"));tools.add(search);tools.add(label("Estado:"));tools.add(filter);tools.add(reset);tools.add(refresh);tools.add(details);
            header.add(tools);summary.setForeground(MUTED);header.add(summary);add(header,BorderLayout.NORTH);

            JPanel detail=panel();detail.setLayout(new BorderLayout(0,10));detail.setBorder(new EmptyBorder(13,13,13,13));
            detail.add(subtitle("Camilla seleccionada"),BorderLayout.NORTH);
            selectedTitle.setOpaque(true);selectedTitle.setBackground(PANEL2);selectedTitle.setForeground(TEXT);selectedTitle.setBorder(new EmptyBorder(9,8,9,8));
            selectedInfo.setRows(9);selectedInfo.setLineWrap(true);selectedInfo.setWrapStyleWord(true);
            JPanel detailsCenter=new JPanel(new BorderLayout(0,9));detailsCenter.setOpaque(false);detailsCenter.add(selectedTitle,BorderLayout.NORTH);detailsCenter.add(scroll(selectedInfo),BorderLayout.CENTER);
            JPanel detailButtons=new JPanel(new GridLayout(0,1,0,7));detailButtons.setOpaque(false);
            JButton openStatus=secondary("Gestionar estado");openStatus.setEnabled(Session.changeStatus());openStatus.setToolTipText(Session.changeStatus()?"Abrir limpieza o falla":"No tienes permiso para cambiar estados.");
            detailButtons.add(action);detailButtons.add(openStatus);detailsCenter.add(detailButtons,BorderLayout.SOUTH);detail.add(detailsCenter,BorderLayout.CENTER);

            JPanel mapWrap=new JPanel(new BorderLayout());mapWrap.setOpaque(false);mapWrap.add(scroll(cards),BorderLayout.CENTER);
            JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,mapWrap,detail);split.setResizeWeight(0.74);split.setDividerLocation(760);split.setBorder(null);split.setBackground(BG);
            add(split,BorderLayout.CENTER);

            onChange(search,this::refresh);filter.addActionListener(e->refresh());
            reset.addActionListener(e->{search.setText("");filter.setSelectedItem("Todos");});
            refresh.addActionListener(e->refresh());details.addActionListener(e->showDetails());
            action.addActionListener(e->runAction());openStatus.addActionListener(e->{if(Session.changeStatus())host.show(new StatusPanel(),"Limpieza / Falla");});
            filter.setSelectedItem(Arrays.asList("Todos","Libre","Ocupada","En limpieza","Falla").contains(initialFilter)?initialFilter:"Todos");
            refresh();
        }

        void refresh(){
            Integer wanted=selected==null?initialSelectedId:selected.id;
            selected=wanted==null?null:findBed(wanted);
            renderCards();updateSelection();
        }

        void renderCards(){
            cards.removeAll();
            String q=search.getText().trim().toLowerCase();String st=Objects.toString(filter.getSelectedItem(),"Todos");
            List<Bed> visible=Store.state.beds.stream().filter(b->q.isBlank()||b.code.toLowerCase().contains(q)||b.area.toLowerCase().contains(q))
                    .filter(b->"Todos".equals(st)||b.status.equals(st)).sorted(Comparator.comparing(b->b.code)).collect(Collectors.toList());
            if(visible.isEmpty()){
                cards.setLayout(new BorderLayout());JLabel empty=new JLabel("No hay camillas que coincidan con la búsqueda.",SwingConstants.CENTER);empty.setForeground(MUTED);cards.add(empty,BorderLayout.CENTER);
            }else{
                cards.setLayout(new GridLayout(0,3,10,10));for(Bed b:visible)cards.add(bedCard(b));
            }
            summary.setText("Mostrando "+visible.size()+" de "+Store.state.beds.size()+" camillas.  Verde: libre | Rojo: ocupada | Amarillo: limpieza | Gris: falla.");
            cards.revalidate();cards.repaint();
        }

        JButton bedCard(Bed b){
            Patient p=b.patientId==null?null:findPatient(b.patientId);boolean active=selected!=null&&selected.id==b.id;
            String who=p==null?b.area:"Paciente: "+p.fullName;
            JButton card=new JButton("<html><b>"+b.code+"</b><br>"+b.status+"<br><font size='2'>"+who+"</font></html>");
            card.setHorizontalAlignment(SwingConstants.LEFT);card.setVerticalAlignment(SwingConstants.CENTER);card.setPreferredSize(new Dimension(190,104));
            Color color=bedColor(b.status);card.setBackground(color);card.setForeground(contrast(color));card.setFocusPainted(false);card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.setBorder(new CompoundBorder(new LineBorder(active?PRIMARY:color.darker(),active?3:1),new EmptyBorder(9,10,9,10)));
            card.addActionListener(e->{selected=b;initialSelectedId=null;renderCards();updateSelection();});return card;
        }

        void updateSelection(){
            if(selected==null){
                selectedTitle.setText("Sin camilla seleccionada");selectedTitle.setBackground(PANEL2);selectedTitle.setForeground(TEXT);
                selectedInfo.setText("Seleccione una tarjeta del mapa para ver el área, el estado, el paciente y las acciones disponibles.");
                action.setText("Acción según estado");action.setEnabled(false);return;
            }
            Patient p=selected.patientId==null?null:findPatient(selected.patientId);Color color=bedColor(selected.status);
            selectedTitle.setText(selected.code+" · "+selected.status);selectedTitle.setBackground(color);selectedTitle.setForeground(contrast(color));
            selectedInfo.setText(bedDetail(selected));
            if("Libre".equals(selected.status)){action.setText("Asignar paciente");action.setEnabled(Session.assign());}
            else if("Ocupada".equals(selected.status)){action.setText("Liberar camilla");action.setEnabled(Session.assign());}
            else{action.setText("Gestionar estado");action.setEnabled(Session.changeStatus());}
            action.setToolTipText(action.isEnabled()?"Aplicar la acción disponible a "+selected.code:"No tienes permiso para esta acción.");
        }

        void showDetails(){if(selected==null){warn("Seleccione una camilla en el mapa.");return;}info(bedDetail(selected));}

        void runAction(){
            if(selected==null){warn("Seleccione una camilla en el mapa.");return;}
            if("Libre".equals(selected.status)){
                if(!Session.assign()){warn("No tienes permiso para asignar camillas.");return;}
                host.show(new AssignPanel(),"Asignar / liberar camilla");return;
            }
            if("Ocupada".equals(selected.status)){
                if(!Session.assign()){warn("No tienes permiso para liberar camillas.");return;}
                if(JOptionPane.showConfirmDialog(this,"¿Liberar "+selected.code+"?","Confirmar liberación",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
                Patient p=selected.patientId==null?null:findPatient(selected.patientId);
                selected.status="Libre";selected.patientId=null;selected.observation="Liberada desde el mapa";selected.updatedAt=LocalDateTime.now();
                Store.save();Store.audit("Liberación de camilla",selected.code+" - "+(p==null?"Paciente":p.fullName));info("Camilla liberada correctamente.");refresh();return;
            }
            if(!Session.changeStatus()){warn("No tienes permiso para cambiar estados.");return;}
            host.show(new StatusPanel(),"Limpieza / Falla");
        }
    }

    // ===================== CAMILLAS =====================
    static class BedsPanel extends JPanel {
        JTextField search=new JTextField();
        JComboBox<String> filter=new JComboBox<>(new String[]{"Todos","Libre","Ocupada","En limpieza","Falla"});
        DefaultTableModel model=ro("ID","Código","Área","Estado","Paciente","Observación","Actualizado");
        JTable table=table(model);
        JLabel summary=new JLabel();
        String initialFilter;

        BedsPanel(){this("Todos");}

        BedsPanel(String initialFilter){
            this.initialFilter=Arrays.asList("Todos","Libre","Ocupada","En limpieza","Falla").contains(initialFilter)?initialFilter:"Todos";
            setBackground(BG); setLayout(new BorderLayout(0,10));
            JPanel header=new JPanel(); header.setOpaque(false); header.setLayout(new BoxLayout(header,BoxLayout.Y_AXIS));
            JLabel t=title("Gestión de camillas"); t.setAlignmentX(LEFT_ALIGNMENT); header.add(t);

            JPanel tools=new JPanel(new FlowLayout(FlowLayout.LEFT,8,8)); tools.setOpaque(false);
            styleField(search); search.setPreferredSize(new Dimension(180,32));
            styleCombo(filter);
            tools.add(label("Buscar:")); tools.add(search);
            tools.add(label("Estado:")); tools.add(filter);

            JButton refresh=primary("Actualizar");
            JButton details=secondary("Ver detalle");
            JButton add=primary("Agregar camilla");
            JButton edit=secondary("Editar");
            JButton delete=secondary("Eliminar");
            JButton release=primary("Liberar ocupada");
            JButton map=secondary("Mapa visual");
            JButton clear=secondary("Limpiar filtros");
            JButton manage=secondary("Cambiar estado");
            tools.add(refresh); tools.add(details);tools.add(map);tools.add(clear);
            if(Session.manageBeds()){ tools.add(add); tools.add(edit); tools.add(delete); }
            if(Session.assign()) tools.add(release);
            if(Session.changeStatus())tools.add(manage);
            header.add(tools);
            summary.setForeground(MUTED); header.add(summary);

            table.setDefaultRenderer(Object.class,new BedRenderer());
            add(header,BorderLayout.NORTH); add(scroll(table),BorderLayout.CENTER);

            onChange(search,this::refresh);
            filter.addActionListener(e->refresh());
            refresh.addActionListener(e->refresh());
            details.addActionListener(e->details());
            add.addActionListener(e->addBed());
            edit.addActionListener(e->editBed());
            delete.addActionListener(e->deleteBed());
            release.addActionListener(e->releaseBed());
            map.addActionListener(e->openMap());
            clear.addActionListener(e->clearFilters());
            manage.addActionListener(e->{MainFrame f=parentFrame(this);if(f!=null)f.show(new StatusPanel(),"Limpieza / Falla");});
            table.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)details();}});
            filter.setSelectedItem(this.initialFilter);
            refresh();
        }

        Integer selectedId(){
            int r=table.getSelectedRow();
            if(r<0) return null;
            r=table.convertRowIndexToModel(r);
            return Integer.parseInt(model.getValueAt(r,0).toString());
        }

        void refresh(){
            model.setRowCount(0);
            String q=search.getText().trim().toLowerCase();
            String st=Objects.toString(filter.getSelectedItem(),"Todos");
            Store.state.beds.stream()
                    .filter(b->q.isBlank()||b.code.toLowerCase().contains(q)||b.area.toLowerCase().contains(q))
                    .filter(b->st.equals("Todos")||b.status.equals(st))
                    .sorted(Comparator.comparing(b->b.code))
                    .forEach(b->{
                        Patient p=b.patientId==null?null:findPatient(b.patientId);
                        model.addRow(new Object[]{b.id,b.code,b.area,b.status,p==null?"-":p.fullName,
                                b.observation==null?"":b.observation,b.updatedAt.format(DF)});
                    });
            summary.setText("Total: "+Store.state.beds.size()+" | Libres: "+countBeds("Libre")+
                    " | Ocupadas: "+countBeds("Ocupada")+" | Limpieza: "+countBeds("En limpieza")+
                    " | Falla: "+countBeds("Falla"));
        }

        void details(){
            Integer id=selectedId(); if(id==null){warn("Seleccione una camilla.");return;}
            info(bedDetail(findBed(id)));
        }

        void clearFilters(){search.setText("");filter.setSelectedItem("Todos");}

        void openMap(){
            MainFrame f=parentFrame(this);
            if(f!=null)f.showBedMap(Objects.toString(filter.getSelectedItem(),"Todos"));
        }

        void addBed(){
            JTextField code=new JTextField(); JTextField area=new JTextField();
            Object[] msg={"Código:",code,"Área:",area};
            if(JOptionPane.showConfirmDialog(this,msg,"Agregar camilla",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
            String c=code.getText().trim(),a=area.getText().trim();
            if(c.isBlank()||a.isBlank()){warn("Complete código y área.");return;}
            if(Store.state.beds.stream().anyMatch(b->b.code.equalsIgnoreCase(c))){warn("El código ya existe.");return;}
            Store.state.beds.add(new Bed(Store.nextBedId(),c,a,"Libre"));
            Store.save(); Store.audit("Alta de camilla","Agregada "+c+" - "+a);
            refresh(); info("Camilla agregada correctamente.");
        }

        void editBed(){
            Integer id=selectedId(); if(id==null){warn("Seleccione una camilla.");return;}
            Bed b=findBed(id);
            JTextField code=new JTextField(b.code); JTextField area=new JTextField(b.area);
            Object[] msg={"Código:",code,"Área:",area};
            if(JOptionPane.showConfirmDialog(this,msg,"Editar camilla",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
            String c=code.getText().trim(),a=area.getText().trim();
            if(c.isBlank()||a.isBlank()){warn("Complete código y área.");return;}
            boolean dup=Store.state.beds.stream().anyMatch(x->x.id!=b.id&&x.code.equalsIgnoreCase(c));
            if(dup){warn("Ese código ya está en uso.");return;}
            String before=b.code+" / "+b.area;
            b.code=c;b.area=a;b.updatedAt=LocalDateTime.now();
            Store.save();Store.audit("Edición de camilla",before+" -> "+c+" / "+a);
            refresh();info("Camilla actualizada.");
        }

        void deleteBed(){
            Integer id=selectedId(); if(id==null){warn("Seleccione una camilla.");return;}
            Bed b=findBed(id);
            if(!"Libre".equals(b.status)){warn("Solo puede eliminar una camilla Libre.");return;}
            if(JOptionPane.showConfirmDialog(this,"¿Eliminar "+b.code+"?","Confirmar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            Store.state.beds.remove(b);Store.save();Store.audit("Eliminación de camilla",b.code);
            refresh();info("Camilla eliminada.");
        }

        void releaseBed(){
            Integer id=selectedId(); if(id==null){warn("Seleccione una camilla.");return;}
            Bed b=findBed(id);
            if(!"Ocupada".equals(b.status)){warn("Seleccione una camilla Ocupada.");return;}
            if(JOptionPane.showConfirmDialog(this,"¿Liberar "+b.code+"?","Confirmar liberación",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            Patient p=b.patientId==null?null:findPatient(b.patientId);
            b.status="Libre";b.patientId=null;b.observation="Alta/liberación de paciente";b.updatedAt=LocalDateTime.now();
            Store.save();Store.audit("Liberación de camilla",b.code+" - "+(p==null?"Paciente":p.fullName));
            refresh();info("Camilla liberada correctamente.");
        }
    }

    // ===================== PACIENTES =====================
    static class PatientsPanel extends JPanel {
        JTextField search=new JTextField();
        DefaultTableModel model=ro("ID","CI","Nombre","Edad","Sexo","Teléfono","Registrado");
        JTable table=table(model);

        PatientsPanel(){
            setBackground(BG);setLayout(new BorderLayout(0,10));
            JPanel header=new JPanel();header.setOpaque(false);header.setLayout(new BoxLayout(header,BoxLayout.Y_AXIS));
            JLabel t=title("Gestión de pacientes");t.setAlignmentX(LEFT_ALIGNMENT);header.add(t);

            JPanel tools=new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));tools.setOpaque(false);
            styleField(search);search.setPreferredSize(new Dimension(220,32));
            JButton add=primary("Nuevo paciente");
            JButton edit=secondary("Editar");
            JButton details=secondary("Ver detalle");
            JButton delete=secondary("Eliminar");
            JButton refresh=primary("Actualizar");
            tools.add(label("Buscar:"));tools.add(search);tools.add(add);tools.add(edit);tools.add(details);
            if(Session.admin())tools.add(delete);
            tools.add(refresh);header.add(tools);

            add(header,BorderLayout.NORTH);add(scroll(table),BorderLayout.CENTER);
            onChange(search,this::refresh);
            add.addActionListener(e->savePatient(null));
            edit.addActionListener(e->edit());
            details.addActionListener(e->details());
            delete.addActionListener(e->delete());
            refresh.addActionListener(e->refresh());
            refresh();
        }

        Integer selectedId(){
            int r=table.getSelectedRow();if(r<0)return null;
            r=table.convertRowIndexToModel(r);
            return Integer.parseInt(model.getValueAt(r,0).toString());
        }

        void refresh(){
            model.setRowCount(0);String q=search.getText().trim().toLowerCase();
            Store.state.patients.stream()
                    .filter(p->q.isBlank()||p.document.toLowerCase().contains(q)||p.fullName.toLowerCase().contains(q))
                    .sorted(Comparator.comparing(p->p.fullName))
                    .forEach(p->model.addRow(new Object[]{p.id,p.document,p.fullName,p.age,p.sex,p.phone,p.createdAt.format(DF)}));
        }

        void savePatient(Patient existing){
            JTextField doc=new JTextField(existing==null?"":existing.document);
            JTextField name=new JTextField(existing==null?"":existing.fullName);
            JSpinner age=new JSpinner(new SpinnerNumberModel(existing==null?0:existing.age,0,120,1));
            JComboBox<String> sex=new JComboBox<>(new String[]{"Masculino","Femenino","Otro"});
            if(existing!=null)sex.setSelectedItem(existing.sex);
            JTextField phone=new JTextField(existing==null?"":existing.phone);
            Object[] msg={"CI / Documento:",doc,"Nombre completo:",name,"Edad:",age,"Sexo:",sex,"Teléfono:",phone};
            String title=existing==null?"Nuevo paciente":"Editar paciente";
            if(JOptionPane.showConfirmDialog(this,msg,title,JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
            String d=doc.getText().trim(),n=name.getText().trim();
            if(d.isBlank()||n.isBlank()){warn("CI y nombre son obligatorios.");return;}
            boolean dup=Store.state.patients.stream().anyMatch(p->(existing==null||p.id!=existing.id)&&p.document.equalsIgnoreCase(d));
            if(dup){warn("Ya existe ese CI.");return;}
            if(existing==null){
                Patient p=new Patient(Store.nextPatientId(),d,n,(Integer)age.getValue(),
                        Objects.toString(sex.getSelectedItem(),""),phone.getText().trim());
                Store.state.patients.add(p);Store.save();Store.audit("Registro de paciente",p.fullName+" - CI "+p.document);
            }else{
                String before=existing.fullName+" / "+existing.document;
                existing.document=d;existing.fullName=n;existing.age=(Integer)age.getValue();
                existing.sex=Objects.toString(sex.getSelectedItem(),"");existing.phone=phone.getText().trim();
                Store.save();Store.audit("Edición de paciente",before+" -> "+n+" / "+d);
            }
            refresh();info("Datos guardados correctamente.");
        }

        void edit(){Integer id=selectedId();if(id==null){warn("Seleccione un paciente.");return;}savePatient(findPatient(id));}
        void details(){
            Integer id=selectedId();if(id==null){warn("Seleccione un paciente.");return;}
            Patient p=findPatient(id);
            long triages=Store.state.triages.stream().filter(t->t.patientId==p.id).count();
            Bed b=Store.state.beds.stream().filter(x->x.patientId!=null&&x.patientId==p.id&&"Ocupada".equals(x.status)).findFirst().orElse(null);
            info("Paciente: "+p.fullName+"\nCI: "+p.document+"\nEdad: "+p.age+"\nSexo: "+p.sex+
                    "\nTeléfono: "+p.phone+"\nCamilla actual: "+(b==null?"-":b.code)+"\nTriajes registrados: "+triages);
        }
        void delete(){
            Integer id=selectedId();if(id==null){warn("Seleccione un paciente.");return;}
            Patient p=findPatient(id);
            boolean hasBed=Store.state.beds.stream().anyMatch(b->b.patientId!=null&&b.patientId==p.id);
            boolean hasTriage=Store.state.triages.stream().anyMatch(t->t.patientId==p.id);
            if(hasBed||hasTriage){warn("No se puede eliminar porque tiene camilla o historial de triaje.");return;}
            if(JOptionPane.showConfirmDialog(this,"¿Eliminar a "+p.fullName+"?","Confirmar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            Store.state.patients.remove(p);Store.save();Store.audit("Eliminación de paciente",p.fullName);
            refresh();info("Paciente eliminado.");
        }
    }

    // ===================== ASIGNAR / LIBERAR =====================
    static class AssignPanel extends JPanel {
        JComboBox<Patient> patient=new JComboBox<>();
        JComboBox<Bed> bed=new JComboBox<>();
        JTextArea info=infoArea();
        DefaultTableModel occupiedModel=ro("Camilla","Área","Paciente","Desde");
        JTable occupied=table(occupiedModel);

        AssignPanel(){
            setBackground(BG);setLayout(new BorderLayout(0,12));
            JPanel top=new JPanel();top.setOpaque(false);top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));
            JLabel t=title("Asignación y liberación");t.setAlignmentX(LEFT_ALIGNMENT);top.add(t);

            JPanel form=panel();form.setLayout(new FlowLayout(FlowLayout.LEFT,10,10));
            styleCombo(patient);styleCombo(bed);patient.setPreferredSize(new Dimension(280,32));bed.setPreferredSize(new Dimension(250,32));
            JButton assign=primary("Asignar");
            JButton release=secondary("Liberar seleccionada");
            JButton refresh=primary("Actualizar");
            JButton map=secondary("Ver mapa");
            form.add(label("Paciente:"));form.add(patient);form.add(label("Camilla libre:"));form.add(bed);
            form.add(assign);form.add(release);form.add(refresh);form.add(map);top.add(form);

            info.setRows(3);top.add(scroll(info));

            JPanel center=new JPanel(new BorderLayout(0,6));center.setOpaque(false);
            center.add(subtitle("Camillas ocupadas"),BorderLayout.NORTH);center.add(scroll(occupied),BorderLayout.CENTER);

            add(top,BorderLayout.NORTH);add(center,BorderLayout.CENTER);
            patient.addActionListener(e->updateInfo());bed.addActionListener(e->updateInfo());
            assign.addActionListener(e->assign());release.addActionListener(e->release());refresh.addActionListener(e->load());
            map.addActionListener(e->{MainFrame f=parentFrame(this);if(f!=null)f.showBedMap("Todos");});
            occupied.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)showOccupiedDetail();}});
            load();
        }

        void load(){
            patient.removeAllItems();Store.state.patients.stream().sorted(Comparator.comparing(p->p.fullName)).forEach(patient::addItem);
            bed.removeAllItems();Store.state.beds.stream().filter(b->"Libre".equals(b.status)).sorted(Comparator.comparing(b->b.code)).forEach(bed::addItem);
            patient.setSelectedIndex(-1);bed.setSelectedIndex(-1);updateInfo();
            occupiedModel.setRowCount(0);
            Store.state.beds.stream().filter(b->"Ocupada".equals(b.status)).sorted(Comparator.comparing(b->b.code)).forEach(b->{
                Patient p=b.patientId==null?null:findPatient(b.patientId);
                occupiedModel.addRow(new Object[]{b.code,b.area,p==null?"-":p.fullName,b.updatedAt.format(DF)});
            });
        }

        void updateInfo(){
            Patient p=(Patient)patient.getSelectedItem();Bed b=(Bed)bed.getSelectedItem();
            info.setText("Paciente: "+(p==null?"-":p.fullName)+"\nCamilla: "+(b==null?"-":b.code+" / "+b.area));
        }

        void assign(){
            Patient p=(Patient)patient.getSelectedItem();Bed b=(Bed)bed.getSelectedItem();
            if(p==null||b==null){warn("Seleccione paciente y camilla.");return;}
            if(!"Libre".equals(b.status)){warn("La camilla ya no está libre.");load();return;}
            boolean already=Store.state.beds.stream().anyMatch(x->x.patientId!=null&&x.patientId==p.id&&"Ocupada".equals(x.status));
            if(already){warn("El paciente ya tiene una camilla asignada.");return;}
            if(JOptionPane.showConfirmDialog(this,"¿Asignar "+b.code+" a "+p.fullName+"?","Confirmar asignación",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            b.status="Ocupada";b.patientId=p.id;b.observation="Asignada a paciente";b.updatedAt=LocalDateTime.now();
            Store.save();Store.audit("Asignación de camilla",b.code+" asignada a "+p.fullName);
            load();info("Asignación correcta.");
        }

        void release(){
            int r=occupied.getSelectedRow();if(r<0){warn("Seleccione una camilla ocupada en la tabla.");return;}
            r=occupied.convertRowIndexToModel(r);
            String code=occupiedModel.getValueAt(r,0).toString();
            Bed b=Store.state.beds.stream().filter(x->x.code.equals(code)).findFirst().orElse(null);
            if(b==null)return;
            if(JOptionPane.showConfirmDialog(this,"¿Liberar "+b.code+"?","Confirmar liberación",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            Patient p=b.patientId==null?null:findPatient(b.patientId);
            b.status="Libre";b.patientId=null;b.observation="Liberada";b.updatedAt=LocalDateTime.now();
            Store.save();Store.audit("Liberación de camilla",b.code+" - "+(p==null?"Paciente":p.fullName));
            load();info("Camilla liberada.");
        }

        void showOccupiedDetail(){
            int r=occupied.getSelectedRow();if(r<0)return;
            r=occupied.convertRowIndexToModel(r);String code=occupiedModel.getValueAt(r,0).toString();
            Bed b=Store.state.beds.stream().filter(x->x.code.equals(code)).findFirst().orElse(null);
            if(b!=null)info(bedDetail(b));
        }
    }

    // ===================== LIMPIEZA / FALLA =====================
    static class StatusPanel extends JPanel {
        JComboBox<Bed> bed=new JComboBox<>();
        JComboBox<String> status=new JComboBox<>(new String[]{"Libre","En limpieza","Falla"});
        JTextArea obs=new JTextArea(5,35);
        JLabel current=new JLabel(" ");

        StatusPanel(){
            setBackground(BG);setLayout(new BorderLayout(0,12));
            add(title("Limpieza, falla y disponibilidad"),BorderLayout.NORTH);
            JPanel form=panel();form.setLayout(new GridBagLayout());form.setBorder(titled("Actualizar camilla"));
            GridBagConstraints g=gbc();styleCombo(bed);styleCombo(status);styleArea(obs);
            addField(form,g,0,0,"Camilla:",bed);
            current.setOpaque(true);current.setForeground(TEXT);current.setBackground(PANEL2);current.setBorder(new EmptyBorder(8,10,8,10));
            addField(form,g,0,1,"Estado actual:",current);
            addField(form,g,0,2,"Nuevo estado:",status);
            g.gridx=0;g.gridy=3;g.anchor=GridBagConstraints.NORTHWEST;form.add(label("Observación:"),g);
            g.gridx=1;g.gridy=3;g.gridwidth=3;g.fill=GridBagConstraints.BOTH;form.add(scroll(obs),g);

            JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));buttons.setOpaque(false);
            JButton save=primary("Guardar cambio");JButton clear=secondary("Limpiar");JButton detail=secondary("Ver detalle");
            JButton cleaning=secondary("Marcar limpieza");JButton failure=secondary("Registrar falla");JButton free=secondary("Marcar libre");JButton map=secondary("Ver mapa");
            buttons.add(save);buttons.add(clear);buttons.add(detail);buttons.add(cleaning);buttons.add(failure);buttons.add(free);buttons.add(map);
            g.gridx=1;g.gridy=4;g.gridwidth=3;form.add(buttons,g);
            add(form,BorderLayout.CENTER);

            bed.addActionListener(e->showCurrent());save.addActionListener(e->saveChange());
            clear.addActionListener(e->{status.setSelectedIndex(0);obs.setText("");});
            detail.addActionListener(e->detail());
            cleaning.addActionListener(e->{status.setSelectedItem("En limpieza");if(obs.getText().isBlank())obs.setText("Limpieza en proceso");});
            failure.addActionListener(e->{status.setSelectedItem("Falla");obs.requestFocus();});
            free.addActionListener(e->{status.setSelectedItem("Libre");if(obs.getText().isBlank())obs.setText("Disponible para asignación");});
            map.addActionListener(e->{MainFrame f=parentFrame(this);if(f!=null)f.showBedMap("Todos");});
            load();
        }

        void load(){
            bed.removeAllItems();Store.state.beds.stream().sorted(Comparator.comparing(b->b.code)).forEach(bed::addItem);
            bed.setSelectedIndex(-1);current.setText(" ");obs.setText("");
        }
        void showCurrent(){
            Bed b=(Bed)bed.getSelectedItem();if(b==null)return;
            current.setText(b.status+" - "+b.area);current.setBackground(bedColor(b.status));current.setForeground(contrast(bedColor(b.status)));obs.setText(b.observation);
        }
        void detail(){
            Bed b=(Bed)bed.getSelectedItem();if(b==null){warn("Seleccione una camilla.");return;}
            info(bedDetail(b));
        }
        void saveChange(){
            Bed b=(Bed)bed.getSelectedItem();String st=Objects.toString(status.getSelectedItem(),"");String o=obs.getText().trim();
            if(b==null){warn("Seleccione una camilla.");return;}
            if("Falla".equals(st)&&o.isBlank()){warn("Para Falla escriba una observación.");return;}
            if("Ocupada".equals(b.status)){
                if(JOptionPane.showConfirmDialog(this,"La camilla está ocupada y se liberará del paciente. ¿Continuar?",
                        "Confirmar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            }
            String before=b.status;b.status=st;b.patientId=null;b.observation=o;b.updatedAt=LocalDateTime.now();
            Store.save();Store.audit("Cambio de estado",b.code+": "+before+" -> "+st+". "+o);
            load();info("Estado actualizado correctamente.");
        }
    }

    // ===================== TRIAJE =====================
    static class TriagePanel extends JPanel {
        String[] levels={"I - Azul - Reanimación","II - Rojo - Emergencia","III - Naranja - Urgente",
                "IV - Verde - Menos urgente","V - Negro - No urgente"};
        JComboBox<Patient> patient=new JComboBox<>();
        JTextField bp=new JTextField();
        JSpinner temp=new JSpinner(new SpinnerNumberModel(36.5,30.0,45.0,0.1));
        JSpinner hr=new JSpinner(new SpinnerNumberModel(80,20,250,1));
        JComboBox<String> level=new JComboBox<>(levels);
        JTextArea notes=new JTextArea(3,25);
        JLabel levelInfo=new JLabel("Seleccione un nivel",SwingConstants.CENTER);
        DefaultTableModel model=ro("ID","Fecha","Paciente","Presión","Temp.","FC","Nivel","Categoría","Tiempo","Notas");
        JTable table=table(model);

        TriagePanel(){
            setBackground(BG);setLayout(new BorderLayout(0,10));
            JPanel top=new JPanel();top.setOpaque(false);top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));
            JLabel t=title("Registro e historial de triaje");t.setAlignmentX(LEFT_ALIGNMENT);top.add(t);

            JPanel form=panel();form.setLayout(new GridBagLayout());form.setBorder(titled("Nuevo triaje"));
            GridBagConstraints g=gbc();styleCombo(patient);styleField(bp);styleCombo(level);styleArea(notes);
            addField(form,g,0,0,"Paciente:",patient);addField(form,g,0,1,"Presión arterial:",bp);
            addField(form,g,0,2,"Temperatura °C:",temp);addField(form,g,0,3,"Frecuencia cardíaca:",hr);
            addField(form,g,2,0,"Nivel:",level);
            levelInfo.setOpaque(true);levelInfo.setBackground(PANEL2);levelInfo.setForeground(TEXT);
            g.gridx=2;g.gridy=1;g.gridwidth=2;g.fill=GridBagConstraints.BOTH;form.add(levelInfo,g);
            g.gridx=2;g.gridy=2;g.gridwidth=1;form.add(label("Notas:"),g);
            g.gridx=3;g.gridy=2;g.gridheight=2;form.add(scroll(notes),g);
            JButton save=primary("Guardar triaje");JButton clear=secondary("Limpiar");
            g.gridx=2;g.gridy=4;g.gridheight=1;form.add(save,g);g.gridx=3;form.add(clear,g);
            top.add(form);

            JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,8,6));buttons.setOpaque(false);
            JButton detail=secondary("Ver detalle");JButton history=primary("Historial del paciente");
            JButton delete=secondary("Eliminar triaje");JButton refresh=primary("Actualizar");
            buttons.add(detail);buttons.add(history);if(Session.admin())buttons.add(delete);buttons.add(refresh);

            JPanel center=new JPanel(new BorderLayout(0,6));center.setOpaque(false);
            center.add(buttons,BorderLayout.NORTH);
            table.setDefaultRenderer(Object.class,new TriageRenderer());
            center.add(scroll(table),BorderLayout.CENTER);

            add(top,BorderLayout.NORTH);add(center,BorderLayout.CENTER);

            level.addActionListener(e->levelInfo());
            save.addActionListener(e->save());
            clear.addActionListener(e->clear());
            detail.addActionListener(e->detail());
            history.addActionListener(e->history());
            delete.addActionListener(e->delete());
            refresh.addActionListener(e->refresh());
            loadPatients();refresh();level.setSelectedIndex(-1);levelInfo();
        }

        void loadPatients(){
            patient.removeAllItems();Store.state.patients.stream().sorted(Comparator.comparing(p->p.fullName)).forEach(patient::addItem);
            patient.setSelectedIndex(-1);
        }
        void clear(){patient.setSelectedIndex(-1);bp.setText("");temp.setValue(36.5);hr.setValue(80);level.setSelectedIndex(-1);notes.setText("");levelInfo();}
        void levelInfo(){
            String l=Objects.toString(level.getSelectedItem(),"");
            if(l.isBlank()){levelInfo.setText("Seleccione un nivel");levelInfo.setBackground(PANEL2);levelInfo.setForeground(TEXT);return;}
            String[] i=triageInfo(l);levelInfo.setText("<html><center>"+i[0]+" | "+i[1]+"<br>"+i[2]+"</center></html>");
            levelInfo.setBackground(triageColor(l));levelInfo.setForeground(l.startsWith("V -")?Color.WHITE:Color.BLACK);
        }
        void save(){
            Patient p=(Patient)patient.getSelectedItem();String pressure=bp.getText().trim();String l=Objects.toString(level.getSelectedItem(),"");
            if(p==null||pressure.isBlank()||l.isBlank()){warn("Complete paciente, presión y nivel.");return;}
            String[] i=triageInfo(l);
            Triage t=new Triage(Store.nextTriageId(),p.id,pressure,((Number)temp.getValue()).doubleValue(),
                    ((Number)hr.getValue()).intValue(),l,i[0],i[1],i[2],notes.getText().trim());
            Store.state.triages.add(t);Store.save();Store.audit("Registro de triaje",p.fullName+" - "+l);
            clear();refresh();info("Triaje guardado correctamente.");
        }
        void refresh(){
            model.setRowCount(0);
            Store.state.triages.stream().sorted(Comparator.comparingInt((Triage t)->priority(t.level)).thenComparing(t->t.createdAt))
                    .forEach(t->{Patient p=findPatient(t.patientId);model.addRow(new Object[]{t.id,t.createdAt.format(DF),
                            p==null?"Desconocido":p.fullName,t.bloodPressure,String.format(Locale.US,"%.1f",t.temperature),
                            t.heartRate,t.level,t.category,t.attentionTime,t.notes});});
        }
        Integer selectedId(){int r=table.getSelectedRow();if(r<0)return null;r=table.convertRowIndexToModel(r);return Integer.parseInt(model.getValueAt(r,0).toString());}
        void detail(){
            Integer id=selectedId();if(id==null){warn("Seleccione un triaje.");return;}
            Triage t=Store.state.triages.stream().filter(x->x.id==id).findFirst().orElse(null);if(t==null)return;
            Patient p=findPatient(t.patientId);
            info("Paciente: "+(p==null?"-":p.fullName)+"\nFecha: "+t.createdAt.format(DF)+"\nPresión: "+t.bloodPressure+
                    "\nTemperatura: "+t.temperature+"\nFrecuencia: "+t.heartRate+"\nNivel: "+t.level+
                    "\nCategoría: "+t.category+"\nTiempo: "+t.attentionTime+"\nNotas: "+t.notes);
        }
        void history(){
            Patient p=(Patient)patient.getSelectedItem();
            if(p==null){
                Integer id=selectedId();
                if(id!=null){
                    Triage t=Store.state.triages.stream().filter(x->x.id==id).findFirst().orElse(null);
                    if(t!=null)p=findPatient(t.patientId);
                }
            }
            if(p==null){warn("Seleccione un paciente o un triaje.");return;}
            final int pid=p.id;
            List<Triage> list=Store.state.triages.stream().filter(t->t.patientId==pid)
                    .sorted(Comparator.comparing((Triage t)->t.createdAt).reversed()).collect(Collectors.toList());
            StringBuilder sb=new StringBuilder("Historial de "+p.fullName+"\n\n");
            for(Triage t:list)sb.append(t.createdAt.format(DF)).append(" | ").append(t.level)
                    .append(" | PA ").append(t.bloodPressure).append(" | T ").append(t.temperature)
                    .append(" | FC ").append(t.heartRate).append("\n");
            info(sb.toString());
        }
        void delete(){
            Integer id=selectedId();if(id==null){warn("Seleccione un triaje.");return;}
            Triage t=Store.state.triages.stream().filter(x->x.id==id).findFirst().orElse(null);if(t==null)return;
            if(JOptionPane.showConfirmDialog(this,"¿Eliminar este registro de triaje?","Confirmar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            Store.state.triages.remove(t);Store.save();Store.audit("Eliminación de triaje","ID "+id);
            refresh();info("Triaje eliminado.");
        }
    }

    // ===================== REPORTES =====================
    static class ReportsPanel extends JPanel {
        DefaultTableModel model=ro();
        JTable table=table(model);
        String current="auditoria";

        ReportsPanel(){
            setBackground(BG);setLayout(new BorderLayout(0,10));
            JPanel header=new JPanel();header.setOpaque(false);header.setLayout(new BoxLayout(header,BoxLayout.Y_AXIS));
            JLabel t=title("Centro de reportes");t.setAlignmentX(LEFT_ALIGNMENT);header.add(t);
            JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));buttons.setOpaque(false);
            JButton audit=primary("Auditoría");JButton triages=primary("Triajes");JButton beds=primary("Camillas");
            JButton patients=primary("Pacientes");JButton csv=secondary("Exportar CSV");
            buttons.add(audit);buttons.add(triages);buttons.add(beds);buttons.add(patients);buttons.add(csv);header.add(buttons);
            add(header,BorderLayout.NORTH);add(scroll(table),BorderLayout.CENTER);

            audit.addActionListener(e->audit());triages.addActionListener(e->triages());beds.addActionListener(e->beds());
            patients.addActionListener(e->patients());csv.addActionListener(e->csv());audit();
        }

        void audit(){
            current="auditoria";model.setColumnIdentifiers(new Object[]{"Fecha","Usuario","Acción","Detalle"});model.setRowCount(0);
            Store.state.audits.stream().sorted(Comparator.comparing((Audit a)->a.createdAt).reversed())
                    .forEach(a->model.addRow(new Object[]{a.createdAt.format(DF),a.username,a.action,a.detail}));
        }
        void triages(){
            current="triajes";model.setColumnIdentifiers(new Object[]{"Fecha","Paciente","Nivel","Categoría","Tiempo"});model.setRowCount(0);
            Store.state.triages.stream().sorted(Comparator.comparing((Triage t)->t.createdAt).reversed()).forEach(t->{
                Patient p=findPatient(t.patientId);model.addRow(new Object[]{t.createdAt.format(DF),p==null?"-":p.fullName,t.level,t.category,t.attentionTime});
            });
        }
        void beds(){
            current="camillas";model.setColumnIdentifiers(new Object[]{"Código","Área","Estado","Paciente","Observación","Actualizado"});model.setRowCount(0);
            Store.state.beds.stream().sorted(Comparator.comparing(b->b.code)).forEach(b->{
                Patient p=b.patientId==null?null:findPatient(b.patientId);model.addRow(new Object[]{b.code,b.area,b.status,p==null?"-":p.fullName,b.observation,b.updatedAt.format(DF)});
            });
        }
        void patients(){
            current="pacientes";model.setColumnIdentifiers(new Object[]{"CI","Nombre","Edad","Sexo","Teléfono"});model.setRowCount(0);
            Store.state.patients.stream().sorted(Comparator.comparing(p->p.fullName))
                    .forEach(p->model.addRow(new Object[]{p.document,p.fullName,p.age,p.sex,p.phone}));
        }
        void csv(){
            if(model.getColumnCount()==0){warn("No hay reporte.");return;}
            JFileChooser fc=new JFileChooser();fc.setSelectedFile(new File("reporte_"+current+".csv"));
            if(fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
            try(BufferedWriter w=Files.newBufferedWriter(fc.getSelectedFile().toPath(),StandardCharsets.UTF_8)){
                for(int c=0;c<model.getColumnCount();c++){if(c>0)w.write(",");w.write(csvCell(model.getColumnName(c)));}w.newLine();
                for(int r=0;r<model.getRowCount();r++){for(int c=0;c<model.getColumnCount();c++){if(c>0)w.write(",");w.write(csvCell(Objects.toString(model.getValueAt(r,c),"")));}w.newLine();}
                Store.audit("Exportación de reporte",fc.getSelectedFile().getName());info("Reporte exportado.");
            }catch(Exception ex){warn("Error exportando: "+ex.getMessage());}
        }
        String csvCell(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    }

    // ===================== HERRAMIENTAS =====================
    static class ToolsPanel extends JPanel {
        ToolsPanel(){
            setBackground(BG);setLayout(new BorderLayout(0,14));
            add(title("Herramientas del administrador"),BorderLayout.NORTH);

            JPanel grid=new JPanel(new GridLayout(2,2,14,14));grid.setOpaque(false);
            grid.add(toolCard("Copia de seguridad","Guardar una copia completa de los datos","Crear backup",this::backup));
            grid.add(toolCard("Restaurar datos","Restaurar una copia creada anteriormente","Restaurar",this::restore));
            grid.add(toolCard("Información del sistema","Ver ruta de datos y estadísticas","Ver información",this::systemInfo));
            grid.add(toolCard("Acerca del proyecto","Resumen de módulos y versión","Acerca de",this::about));
            add(grid,BorderLayout.CENTER);
        }

        JPanel toolCard(String title,String desc,String button,Runnable action){
            JPanel p=panel();p.setLayout(new BorderLayout(0,10));p.setBorder(new EmptyBorder(20,20,20,20));
            JLabel t=subtitle(title);JLabel d=new JLabel("<html>"+desc+"</html>");d.setForeground(MUTED);
            JButton b=primary(button);b.addActionListener(e->action.run());
            p.add(t,BorderLayout.NORTH);p.add(d,BorderLayout.CENTER);p.add(b,BorderLayout.SOUTH);return p;
        }
        void backup(){
            JFileChooser fc=new JFileChooser();fc.setSelectedFile(new File("hospital_backup.bin"));
            if(fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
            try{Store.backup(fc.getSelectedFile());Store.audit("Backup","Copia creada: "+fc.getSelectedFile().getName());info("Copia de seguridad creada.");}
            catch(Exception ex){warn("No se pudo crear la copia: "+ex.getMessage());}
        }
        void restore(){
            JFileChooser fc=new JFileChooser();
            if(fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
            if(JOptionPane.showConfirmDialog(this,"Se reemplazarán los datos actuales. ¿Continuar?","Restaurar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            try{Store.restore(fc.getSelectedFile());Store.audit("Restauración","Datos restaurados");info("Datos restaurados. Cambie de módulo para refrescar.");}
            catch(Exception ex){warn("No se pudo restaurar: "+ex.getMessage());}
        }
        void systemInfo(){
            info("Ruta de datos:\n"+Store.FILE+"\n\nCamillas: "+Store.state.beds.size()+
                    "\nPacientes: "+Store.state.patients.size()+"\nTriajes: "+Store.state.triages.size()+
                    "\nRegistros de auditoría: "+Store.state.audits.size());
        }
        void about(){info("Sistema Hospitalario PRO\nProyecto final\nJava Swing\nTema oscuro\nVersión 2.0");}
    }

    // ===================== RENDERERS =====================
    static class BedRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean focus,int row,int col){
            Component c=super.getTableCellRendererComponent(t,v,sel,focus,row,col);
            String st=Objects.toString(t.getValueAt(row,3),"");
            if(!sel){c.setBackground(bedColor(st));c.setForeground("Falla".equals(st)?TEXT:Color.BLACK);}
            return c;
        }
    }
    static class TriageRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean focus,int row,int col){
            Component c=super.getTableCellRendererComponent(t,v,sel,focus,row,col);
            String l=Objects.toString(t.getValueAt(row,6),"");
            if(!sel){c.setBackground(triageColor(l));c.setForeground(l.startsWith("V -")?Color.WHITE:Color.BLACK);}
            return c;
        }
    }

    // ===================== HELPERS =====================
    static Patient findPatient(int id){return Store.state.patients.stream().filter(p->p.id==id).findFirst().orElse(null);}
    static Bed findBed(int id){return Store.state.beds.stream().filter(b->b.id==id).findFirst().orElse(null);}
    static MainFrame parentFrame(Component c){
        Window w=SwingUtilities.getWindowAncestor(c);
        return w instanceof MainFrame?(MainFrame)w:null;
    }
    static String bedDetail(Bed b){
        if(b==null)return "Camilla no encontrada.";
        Patient p=b.patientId==null?null:findPatient(b.patientId);
        return "Código: "+b.code+"\nÁrea: "+b.area+"\nEstado: "+b.status+
                "\nPaciente: "+(p==null?"-":p.fullName)+"\nObservación: "+Objects.toString(b.observation,"")+
                "\nÚltimo cambio: "+b.updatedAt.format(DF);
    }
    static Color contrast(Color c){
        int light=(c.getRed()*299+c.getGreen()*587+c.getBlue()*114)/1000;
        return light>=150?Color.BLACK:TEXT;
    }
    static int countBeds(String st){return (int)Store.state.beds.stream().filter(b->b.status.equals(st)).count();}
    static int priority(String l){if(l.startsWith("I -"))return 1;if(l.startsWith("II -"))return 2;if(l.startsWith("III -"))return 3;if(l.startsWith("IV -"))return 4;return 5;}
    static String[] triageInfo(String l){
        if(l.startsWith("I -"))return new String[]{"Azul","Reanimación","Inmediato"};
        if(l.startsWith("II -"))return new String[]{"Rojo","Emergencia","Inmediato / médicos 7 min"};
        if(l.startsWith("III -"))return new String[]{"Naranja","Urgente","30 minutos"};
        if(l.startsWith("IV -"))return new String[]{"Verde","Menos urgente","45 minutos"};
        return new String[]{"Negro","No urgente","60 minutos"};
    }
    static Color bedColor(String s){
        return switch(s){case "Libre"->new Color(94,190,125);case "Ocupada"->new Color(225,105,115);
            case "En limpieza"->new Color(230,190,80);case "Falla"->new Color(90,95,108);default->PANEL2;};
    }
    static Color triageColor(String l){
        if(l.startsWith("I -"))return new Color(105,177,235);if(l.startsWith("II -"))return new Color(230,100,112);
        if(l.startsWith("III -"))return new Color(235,145,60);if(l.startsWith("IV -"))return new Color(90,185,120);
        if(l.startsWith("V -"))return new Color(40,40,40);return PANEL2;
    }
    static String sha256(String s){
        try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] d=md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder b=new StringBuilder();for(byte x:d)b.append(String.format("%02x",x));return b.toString();}
        catch(Exception e){throw new RuntimeException(e);}
    }

    static void darkDefaults(){
        UIManager.put("Panel.background",PANEL);UIManager.put("OptionPane.background",PANEL);
        UIManager.put("OptionPane.messageForeground",TEXT);UIManager.put("Label.foreground",TEXT);
        UIManager.put("TextField.background",PANEL2);UIManager.put("TextField.foreground",TEXT);
        UIManager.put("TextField.caretForeground",TEXT);UIManager.put("PasswordField.background",PANEL2);
        UIManager.put("PasswordField.foreground",TEXT);UIManager.put("ComboBox.background",PANEL2);
        UIManager.put("ComboBox.foreground",TEXT);UIManager.put("Table.background",PANEL2);
        UIManager.put("Table.foreground",TEXT);UIManager.put("Table.gridColor",new Color(55,62,73));
        UIManager.put("TableHeader.background",new Color(34,39,49));UIManager.put("TableHeader.foreground",TEXT);
    }
    static JPanel panel(){JPanel p=new JPanel();p.setBackground(PANEL);return p;}
    static JLabel label(String s){JLabel l=new JLabel(s);l.setForeground(TEXT);return l;}
    static JLabel title(String s){JLabel l=new JLabel(s);l.setForeground(TEXT);l.setFont(new Font("Segoe UI",Font.BOLD,22));return l;}
    static JLabel subtitle(String s){JLabel l=new JLabel(s);l.setForeground(TEXT);l.setFont(new Font("Segoe UI",Font.BOLD,15));return l;}
    static JButton primary(String s){
        JButton b=new JButton(s);b.setBackground(PRIMARY);b.setForeground(Color.WHITE);b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(new Font("Segoe UI",Font.BOLD,12));b.setBorder(new EmptyBorder(9,14,9,14));return b;
    }
    static JButton secondary(String s){
        JButton b=new JButton(s);b.setBackground(new Color(67,74,87));b.setForeground(Color.WHITE);b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(new Font("Segoe UI",Font.PLAIN,12));b.setBorder(new EmptyBorder(9,14,9,14));return b;
    }
    static void styleField(JTextField f){f.setBackground(PANEL2);f.setForeground(TEXT);f.setCaretColor(TEXT);f.setBorder(new CompoundBorder(new LineBorder(new Color(62,70,84)),new EmptyBorder(7,9,7,9)));}
    static void styleArea(JTextArea a){a.setBackground(PANEL2);a.setForeground(TEXT);a.setCaretColor(TEXT);a.setLineWrap(true);a.setWrapStyleWord(true);a.setBorder(new EmptyBorder(7,7,7,7));}
    static JTextArea infoArea(){JTextArea a=new JTextArea();styleArea(a);a.setEditable(false);return a;}
    static void styleCombo(JComboBox<?> c){c.setBackground(PANEL2);c.setForeground(TEXT);c.setPreferredSize(new Dimension(220,32));}
    static JTable table(DefaultTableModel m){JTable t=new JTable(m);t.setRowHeight(29);t.setAutoCreateRowSorter(true);t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);t.setBackground(PANEL2);t.setForeground(TEXT);t.setGridColor(new Color(52,60,72));t.setSelectionBackground(PRIMARY);t.setSelectionForeground(Color.WHITE);t.getTableHeader().setBackground(new Color(34,39,49));t.getTableHeader().setForeground(TEXT);t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12));return t;}
    static JScrollPane scroll(Component c){JScrollPane s=new JScrollPane(c);s.getViewport().setBackground(PANEL2);s.setBorder(new LineBorder(new Color(52,60,72)));return s;}
    static DefaultTableModel ro(String...cols){return new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};}
    static GridBagConstraints gbc(){GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(7,8,7,8);g.anchor=GridBagConstraints.WEST;g.fill=GridBagConstraints.HORIZONTAL;return g;}
    static Border titled(String s){return new CompoundBorder(new TitledBorder(new LineBorder(new Color(58,65,78)),s,TitledBorder.LEFT,TitledBorder.TOP,null,TEXT),new EmptyBorder(10,10,10,10));}
    static void addField(JPanel p,GridBagConstraints g,int x,int y,String label,Component c){g.gridx=x;g.gridy=y;g.gridwidth=1;g.weightx=0;p.add(HospitalProApp.label(label),g);g.gridx=x+1;g.weightx=1;if(c instanceof JTextField f)styleField(f);if(c instanceof JComboBox<?> cb)styleCombo(cb);p.add(c,g);}
    static void onChange(JTextField f,Runnable r){f.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){r.run();}public void removeUpdate(DocumentEvent e){r.run();}public void changedUpdate(DocumentEvent e){r.run();}});}
    static void warn(String s){JOptionPane.showMessageDialog(null,s,"Validación",JOptionPane.WARNING_MESSAGE);}
    static void info(String s){JOptionPane.showMessageDialog(null,s,"Información",JOptionPane.INFORMATION_MESSAGE);}
}
