package eventmanagementapp;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// ======================== INTERFACES ========================

interface Bookable {
    boolean book(Attendee attendee, int seats);
    int getAvailableSeats();
}

interface Cancellable {
    boolean cancel(Attendee attendee, int seats);
}

// ======================== ABSTRACT BASE CLASS ========================

abstract class BaseEvent implements Serializable, Bookable, Cancellable {

    protected String eventId;
    protected String name;
    protected String date;
    protected Venue venue;
    protected int[] seatCapacity;
    protected int bookedSeats;

    private transient ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public BaseEvent(String eventId, String name, String date, Venue venue, int totalSeats) {
        this.eventId = eventId;
        this.name = name;
        this.date = date;
        this.venue = venue;

        if (totalSeats > venue.getCapacity()) {
            throw new IllegalArgumentException("Seats exceed venue capacity");
        }

        this.seatCapacity = new int[totalSeats];
        this.bookedSeats = 0;
    }

    public abstract String getEventType();

    @Override
    public boolean book(Attendee attendee, int seats) {
        lock.writeLock().lock();
        try {
            if (getAvailableSeats() >= seats) {

                // ✅ FIX: array + type castingP
                Object obj = seatCapacity;
                int[] arr = (int[]) obj;

                int count = 0;
                for (int i = 0; i < arr.length && count < seats; i++) {
                    if (arr[i] == 0) {
                        arr[i] = 1;
                        count++;
                    }
                }

                bookedSeats += seats;
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean cancel(Attendee attendee, int seats) {
        lock.writeLock().lock();
        try {
            if (bookedSeats >= seats) {

                Object obj = seatCapacity;
                int[] arr = (int[]) obj;

                int count = 0;
                for (int i = arr.length - 1; i >= 0 && count < seats; i--) {
                    if (arr[i] == 1) {
                        arr[i] = 0;
                        count++;
                    }
                }

                bookedSeats -= seats;
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int getAvailableSeats() {
        return seatCapacity.length - bookedSeats;
    }

    private void readObject(ObjectInputStream in) throws Exception {
        in.defaultReadObject();
        lock = new ReentrantReadWriteLock();
    }

    public String getEventId() { return eventId; }
    public String getName() { return name; }
    public String getDate() { return date; }
    public Venue getVenue() { return venue; }
    public int getTotalSeats() { return seatCapacity.length; }
    public int getBookedSeats() { return bookedSeats; }
}

// ======================== CONCRETE EVENT CLASSES ========================

class ConcertEvent extends BaseEvent {
    private static final long serialVersionUID = 1L;
    private String artist;

    public ConcertEvent(String id, String name, String date, Venue venue, int seats, String artist) {
        super(id, name, date, venue, seats);
        this.artist = artist;
    }

    @Override public String getEventType() { return "Concert"; }
    public String getArtist() { return artist; }
}

class ConferenceEvent extends BaseEvent {
    private static final long serialVersionUID = 1L;
    private String topic;

    public ConferenceEvent(String id, String name, String date, Venue venue, int seats, String topic) {
        super(id, name, date, venue, seats);
        this.topic = topic;
    }

    @Override public String getEventType() { return "Conference"; }
    public String getTopic() { return topic; }
}

// ======================== VENUE ========================

class Venue implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String venueId, name, location;
    private final int capacity;

    public Venue(String venueId, String name, String location, int capacity) {
        this.venueId  = venueId;
        this.name     = name;
        this.location = location;
        this.capacity = capacity;
    }

    public String getName()     { return name; }
    public String getLocation() { return location; }
    public int    getCapacity() { return capacity; }

    @Override public String toString() { return name + ", " + location; }
}

// ======================== TICKET ========================

class Ticket implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String ticketId;
    private final BaseEvent event;
    private final Attendee attendee;
    private final int numberOfSeats;
    private final double price;
    private String status; // CONFIRMED | CANCELLED

    public Ticket(String ticketId, BaseEvent event, Attendee attendee, int numberOfSeats, double price) {
        this.ticketId      = ticketId;
        this.event         = event;
        this.attendee      = attendee;
        this.numberOfSeats = numberOfSeats;
        this.price         = price;
        this.status        = "CONFIRMED";
    }

    public void   cancel()              { this.status = "CANCELLED"; }
    public String getTicketId()         { return ticketId; }
    public BaseEvent getEvent()         { return event; }
    public Attendee  getAttendee()      { return attendee; }
    public int    getNumberOfSeats()    { return numberOfSeats; }
    public double getPrice()            { return price; }
    public String getStatus()           { return status; }

    @Override
    public String toString() {
        return "Ticket[" + ticketId + "] " + event.getName() + " x" + numberOfSeats + " (" + status + ")";
    }
}

// ======================== ATTENDEE ========================

class Attendee implements Serializable, Comparable<Attendee> {
    private static final long serialVersionUID = 1L;
    private final String attendeeId, name, email;
    private final List<Ticket> tickets = new ArrayList<>();

    public Attendee(String attendeeId, String name, String email) {
        this.attendeeId = attendeeId;
        this.name       = name;
        this.email      = email;
    }

    public void addTicket(Ticket t)              { tickets.add(t); }
    public String      getAttendeeId()           { return attendeeId; }
    public String      getName()                 { return name; }
    public String      getEmail()                { return email; }
    public List<Ticket> getTickets()             { return tickets; }

    @Override public int compareTo(Attendee o)   { return name.compareTo(o.name); }
    @Override public String toString()           { return name + " (" + email + ")"; }
}

// ======================== EVENT REGISTRY ========================

class EventRegistry {
    // HashMap<EventId, TreeSet<Attendee>> — sorted attendee set per event
    private final HashMap<String, TreeSet<Attendee>> attendeeRegistry = new HashMap<>();
    private List<BaseEvent> events       = new ArrayList<>();
    private List<Ticket>    allTickets   = new ArrayList<>();
    private int ticketCounter = 1;

    public void addEvent(BaseEvent event) {
        events.add(event);
        attendeeRegistry.put(event.getEventId(), new TreeSet<>());
    }

    public Ticket bookTicket(BaseEvent event, Attendee attendee, int seats, double pricePerSeat) {
        if (event.book(attendee, seats)) {
            String id     = "TKT" + String.format("%03d", ticketCounter++);
            Ticket ticket = new Ticket(id, event, attendee, seats, seats * pricePerSeat);
            attendee.addTicket(ticket);
            allTickets.add(ticket);
            attendeeRegistry.get(event.getEventId()).add(attendee);
            return ticket;
        }
        return null;
    }

    public boolean cancelTicket(Ticket ticket) {
        BaseEvent event   = ticket.getEvent();
        Attendee  attendee = ticket.getAttendee();
        if (event.cancel(attendee, ticket.getNumberOfSeats())) {
            ticket.cancel();
            boolean hasActive = attendee.getTickets().stream()
                    .anyMatch(t -> t.getEvent().getEventId().equals(event.getEventId())
                            && "CONFIRMED".equals(t.getStatus()));
            if (!hasActive) attendeeRegistry.get(event.getEventId()).remove(attendee);
            return true;
        }
        return false;
    }

    // Serialize events + tickets via ObjectOutputStream
    public void serializeToFile(String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(events);
            oos.writeObject(allTickets);
            System.out.println("Serialized to " + filename);
        }
    }

    @SuppressWarnings("unchecked")
    public void deserializeFromFile(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            events     = (List<BaseEvent>) ois.readObject();
            allTickets = (List<Ticket>)    ois.readObject();
            // Rebuild attendee registry after loading
            attendeeRegistry.clear();
            for (BaseEvent e : events) attendeeRegistry.put(e.getEventId(), new TreeSet<>());
            for (Ticket t : allTickets) {
                if ("CONFIRMED".equals(t.getStatus()))
                    attendeeRegistry.get(t.getEvent().getEventId()).add(t.getAttendee());
            }
        }
    }

    public List<BaseEvent> getEvents()                                  { return events; }
    public List<Ticket>    getAllTickets()                               { return allTickets; }
    public TreeSet<Attendee> getAttendeesForEvent(String eventId)       {
        return attendeeRegistry.getOrDefault(eventId, new TreeSet<>());
    }
}

// ======================== CHART PANEL (Graphics2D) ========================

class AttendanceChartPanel extends JPanel {
    private List<BaseEvent> events;

    public AttendanceChartPanel(List<BaseEvent> events) {
        this.events = events;
        setBackground(Color.WHITE);
    }

    public void refresh(List<BaseEvent> events) { this.events = events; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int pad = 60, w = getWidth() - 2 * pad, h = getHeight() - 2 * pad - 30;

        if (events.isEmpty()) {
            g2.drawString("No events to display", getWidth() / 2 - 60, getHeight() / 2);
            return;
        }

        int maxSeats = events.stream().mapToInt(BaseEvent::getTotalSeats).max().orElse(1);

        // Draw axes
        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(pad, pad, pad, pad + h);
        g2.drawLine(pad, pad + h, pad + w, pad + h);

        // Title
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString("Event Attendance Overview", getWidth() / 2 - 100, 20);

        // Y grid lines
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        for (int i = 0; i <= 5; i++) {
            int y   = pad + h - i * h / 5;
            int val = i * maxSeats / 5;
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(pad, y, pad + w, y);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(String.valueOf(val), pad - 38, y + 4);
        }

        Color[] palette = {
                new Color(66, 133, 244), new Color(234, 67, 53),
                new Color(52, 168, 83),  new Color(251, 188, 5),
                new Color(103, 58, 183)
        };

        int barW = Math.max(20, w / (events.size() * 3));
        int x    = pad + 20;

        for (int i = 0; i < events.size(); i++) {
            BaseEvent ev = events.get(i);
            Color c      = palette[i % palette.length];

            // Booked bar
            int bH = (int) ((double) ev.getBookedSeats()   / maxSeats * h);
            g2.setColor(c);
            g2.fillRoundRect(x, pad + h - bH, barW, bH, 6, 6);

            // Available bar (lighter)
            int aH = (int) ((double) ev.getAvailableSeats() / maxSeats * h);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
            g2.fillRoundRect(x + barW + 4, pad + h - aH, barW, aH, 6, 6);
            g2.setColor(c.darker());
            g2.drawRoundRect(x + barW + 4, pad + h - aH, barW, aH, 6, 6);

            // X label
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
            String label = ev.getName().length() > 9 ? ev.getName().substring(0, 9) + ".." : ev.getName();
            g2.drawString(label, x - 2, pad + h + 14);

            x += barW * 2 + 28;
        }

        // Legend
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(new Color(66, 133, 244));
        g2.fillRect(pad, getHeight() - 18, 11, 11);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Booked", pad + 14, getHeight() - 8);

        g2.setColor(new Color(66, 133, 244, 70));
        g2.fillRect(pad + 80, getHeight() - 18, 11, 11);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Available", pad + 94, getHeight() - 8);
    }
}

// ======================== MAIN SWING APPLICATION ========================

public class EventManagementApp extends JFrame {

    private final EventRegistry registry  = new EventRegistry();
    private final List<Attendee> attendees = new ArrayList<>();

    private DefaultTableModel eventsModel, bookingsModel, reportModel;
    private AttendanceChartPanel chartPanel;

    public static void main(String[] args) {
        // -- Concurrent booking demo (console) --
        runConcurrentDemo();
        // -- Launch GUI --
        SwingUtilities.invokeLater(EventManagementApp::new);
    }

    // ---- Constructor ----

    public EventManagementApp() {
        super("Event Management Platform");
        loadSampleData();
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1050, 720);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ---- Sample data ----

    private void loadSampleData() {
        Venue v1 = new Venue("V001", "Grand Arena",   "Chennai",    500);
        Venue v2 = new Venue("V002", "Tech Hub",      "Bangalore",  300);
        Venue v3 = new Venue("V003", "City Hall",     "Mumbai",     200);
        Venue v4 = new Venue("V004", "Open Grounds",  "Delhi",      400);

        BaseEvent e1 = new ConcertEvent   ("E001", "Rock Night",    "2025-06-10", v1, 500, "The Rockers");
        BaseEvent e2 = new ConferenceEvent("E002", "Java Summit",   "2025-06-15", v2, 300, "Java 21 Features");
        BaseEvent e3 = new ConcertEvent   ("E003", "Jazz Evening",  "2025-06-20", v3, 200, "Jazz Masters");
        BaseEvent e4 = new ConferenceEvent("E004", "AI Conference", "2025-06-25", v4, 350, "Future of AI");

        for (BaseEvent e : new BaseEvent[]{e1, e2, e3, e4}) registry.addEvent(e);

        Attendee a1 = new Attendee("A001", "Alice Johnson", "alice@mail.com");
        Attendee a2 = new Attendee("A002", "Bob Smith",     "bob@mail.com");
        Attendee a3 = new Attendee("A003", "Carol White",   "carol@mail.com");
        Attendee a4 = new Attendee("A004", "David Brown",   "david@mail.com");

        attendees.addAll(List.of(a1, a2, a3, a4));

        registry.bookTicket(e1, a1, 2,  500);
        registry.bookTicket(e1, a2, 3,  500);
        registry.bookTicket(e2, a3, 1, 1000);
        registry.bookTicket(e3, a4, 4,  750);
        registry.bookTicket(e2, a1, 2, 1000);
        registry.bookTicket(e4, a2, 1, 1200);
        registry.bookTicket(e4, a3, 5, 1200);
    }

    // ---- Build UI ----

    private void buildUI() {
        // Menu
        JMenuBar mb  = new JMenuBar();
        JMenu file   = new JMenu("File");
        JMenuItem sv = new JMenuItem("💾 Save Events");
        JMenuItem ld = new JMenuItem("📂 Load Events");
        JMenuItem ex = new JMenuItem("Exit");
        sv.addActionListener(e -> saveEvents());
        ld.addActionListener(e -> loadEvents());
        ex.addActionListener(e -> System.exit(0));
        file.add(sv); file.add(ld); file.addSeparator(); file.add(ex);
        mb.add(file);
        setJMenuBar(mb);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.addTab("🎟  Events",   buildEventsTab());
        tabs.addTab("📋  Bookings", buildBookingsTab());
        tabs.addTab("📊  Reports",  buildReportsTab());

        tabs.addChangeListener(e -> { if (tabs.getSelectedIndex() == 2) refreshReports(); });

        add(tabs);
    }

    // ======================== EVENTS TAB ========================

    private JPanel buildEventsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Attendees panel (north) ---
        JTextArea attArea = new JTextArea(4, 30);
        attArea.setEditable(false);
        attArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane attScroll = new JScrollPane(attArea);
        attScroll.setBorder(BorderFactory.createTitledBorder("Attendees for selected event (sorted by name via TreeSet)"));
        panel.add(attScroll, BorderLayout.NORTH);

        // --- Table ---
        String[] cols = {"ID", "Name", "Type", "Date", "Venue", "Total", "Booked", "Available", "Capacity %"};
        eventsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(eventsModel);
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        // JProgressBar renderer for capacity column
        table.getColumnModel().getColumn(8).setCellRenderer(
                (tbl, val, sel, foc, row, col) -> {
                    JProgressBar bar = new JProgressBar(0, 100);
                    int pct = (Integer) val;
                    bar.setValue(pct);
                    bar.setStringPainted(true);
                    bar.setString(pct + "%");
                    bar.setForeground(pct > 80 ? new Color(220,53,69)
                            : pct > 50 ? new Color(255,140,0)
                            : new Color(40,167,69));
                    return bar;
                }
        );

        refreshEventsTable();
        JScrollPane scroll = new JScrollPane(table);
        panel.add(scroll, BorderLayout.CENTER);

        // Selection listener → show attendees in TreeSet order
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                String eid = (String) eventsModel.getValueAt(table.getSelectedRow(), 0);
                TreeSet<Attendee> set = registry.getAttendeesForEvent(eid);
                StringBuilder sb = new StringBuilder();
                set.forEach(a -> sb.append("  • ").append(a.getName())
                        .append("  |  ").append(a.getEmail()).append("\n"));
                attArea.setText(sb.length() > 0 ? sb.toString() : "  (no attendees yet)");
            }
        });

        // --- Buttons ---
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JButton btnAddEvent    = btn("+ Add Event",    new Color(13,110,253));
        JButton btnAddAttendee = btn("+ Attendee",     new Color(25,135,84));
        JButton btnBook        = btn("Book Ticket",    new Color(255,140,0));
        JButton btnCancel      = btn("Cancel Ticket",  new Color(220,53,69));
        JButton btnRefresh     = btn("↺ Refresh",      Color.GRAY);

        btnAddEvent.addActionListener   (e -> showAddEventDialog());
        btnAddAttendee.addActionListener(e -> showAddAttendeeDialog());
        btnBook.addActionListener       (e -> showBookingDialog());
        btnCancel.addActionListener     (e -> showCancelDialog());
        btnRefresh.addActionListener    (e -> refreshEventsTable());

        btns.add(btnAddEvent); btns.add(btnAddAttendee);
        btns.add(btnBook);     btns.add(btnCancel); btns.add(btnRefresh);
        panel.add(btns, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshEventsTable() {
        eventsModel.setRowCount(0);
        for (BaseEvent ev : registry.getEvents()) {
            int pct = ev.getTotalSeats() == 0 ? 0
                    : (int)((double) ev.getBookedSeats() / ev.getTotalSeats() * 100);
            eventsModel.addRow(new Object[]{
                    ev.getEventId(), ev.getName(), ev.getEventType(), ev.getDate(),
                    ev.getVenue().toString(), ev.getTotalSeats(),
                    ev.getBookedSeats(), ev.getAvailableSeats(), pct
            });
        }
    }

    // ======================== BOOKINGS TAB ========================

    private JPanel buildBookingsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"Ticket ID", "Event", "Attendee", "Seats", "Price (₹)", "Status"};
        bookingsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(bookingsModel);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        // Colour-coded status renderer
        table.getColumnModel().getColumn(5).setCellRenderer(
                (tbl, val, sel, foc, row, col) -> {
                    JLabel lbl = new JLabel(String.valueOf(val), SwingConstants.CENTER);
                    lbl.setOpaque(true);
                    boolean ok = "CONFIRMED".equals(val);
                    lbl.setBackground(ok ? new Color(212,237,218) : new Color(248,215,218));
                    lbl.setForeground(ok ? new Color(21,87,36)    : new Color(114,28,36));
                    lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
                    return lbl;
                }
        );

        refreshBookingsTable();
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = btn("↺ Refresh", Color.GRAY);
        refresh.addActionListener(e -> refreshBookingsTable());
        btns.add(refresh);
        panel.add(btns, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshBookingsTable() {
        bookingsModel.setRowCount(0);
        for (Ticket t : registry.getAllTickets()) {
            bookingsModel.addRow(new Object[]{
                    t.getTicketId(), t.getEvent().getName(), t.getAttendee().getName(),
                    t.getNumberOfSeats(), String.format("%.2f", t.getPrice()), t.getStatus()
            });
        }
    }

    // ======================== REPORTS TAB ========================

    private JPanel buildReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chartPanel = new AttendanceChartPanel(registry.getEvents());
        chartPanel.setBorder(BorderFactory.createTitledBorder("Attendance Chart (Graphics2D)"));
        panel.add(chartPanel, BorderLayout.CENTER);

        String[] cols = {"Event", "Type", "Total", "Booked", "Available", "Revenue (₹)"};
        reportModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable rt = new JTable(reportModel);
        rt.setRowHeight(26);
        rt.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        JScrollPane rScroll = new JScrollPane(rt);
        rScroll.setPreferredSize(new Dimension(900, 140));
        rScroll.setBorder(BorderFactory.createTitledBorder("Revenue Summary"));
        panel.add(rScroll, BorderLayout.SOUTH);

        refreshReports();
        return panel;
    }

    private void refreshReports() {
        if (chartPanel != null) chartPanel.refresh(registry.getEvents());
        if (reportModel == null) return;
        reportModel.setRowCount(0);
        for (BaseEvent ev : registry.getEvents()) {
            double rev = registry.getAllTickets().stream()
                    .filter(t -> t.getEvent().getEventId().equals(ev.getEventId())
                            && "CONFIRMED".equals(t.getStatus()))
                    .mapToDouble(Ticket::getPrice).sum();
            reportModel.addRow(new Object[]{
                    ev.getName(), ev.getEventType(), ev.getTotalSeats(),
                    ev.getBookedSeats(), ev.getAvailableSeats(), String.format("%.2f", rev)
            });
        }
    }

    // ======================== DIALOGS ========================

    private void showAddEventDialog() {
        JDialog d = new JDialog(this, "Add New Event", true);
        d.setSize(420, 360);
        d.setLocationRelativeTo(this);
        d.setLayout(new GridLayout(0, 2, 10, 10));
        d.getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField fName  = new JTextField();
        JTextField fDate  = new JTextField("2025-07-15");
        JComboBox<String> fType  = new JComboBox<>(new String[]{"Concert", "Conference"});
        JTextField fSeats = new JTextField("100");
        JTextField fVName = new JTextField();
        JTextField fVLoc  = new JTextField();
        JTextField fExtra = new JTextField();
        JLabel     lExtra = new JLabel("Artist:");
        fType.addActionListener(e -> lExtra.setText("Concert".equals(fType.getSelectedItem()) ? "Artist:" : "Topic:"));

        d.add(new JLabel("Name:"));  d.add(fName);
        d.add(new JLabel("Date:"));  d.add(fDate);
        d.add(new JLabel("Type:"));  d.add(fType);
        d.add(new JLabel("Seats:")); d.add(fSeats);
        d.add(new JLabel("Venue:")); d.add(fVName);
        d.add(new JLabel("City:"));  d.add(fVLoc);
        d.add(lExtra);               d.add(fExtra);

        JButton ok = btn("Add", new Color(13,110,253));
        ok.addActionListener(e -> {
            try {
                String id = "E" + String.format("%03d", registry.getEvents().size() + 1);
                int seats = Integer.parseInt(fSeats.getText().trim());
                Venue v   = new Venue("V" + id, fVName.getText().trim(), fVLoc.getText().trim(), seats);
                BaseEvent ev = "Concert".equals(fType.getSelectedItem())
                        ? new ConcertEvent   (id, fName.getText().trim(), fDate.getText().trim(), v, seats, fExtra.getText().trim())
                        : new ConferenceEvent(id, fName.getText().trim(), fDate.getText().trim(), v, seats, fExtra.getText().trim());
                registry.addEvent(ev);
                refreshEventsTable();
                JOptionPane.showMessageDialog(d, "Event added!");
                d.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(d, "Enter a valid seat count.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        d.add(new JLabel()); d.add(ok);
        d.setVisible(true);
    }

    private void showAddAttendeeDialog() {
        JDialog d = new JDialog(this, "Add Attendee", true);
        d.setSize(360, 200);
        d.setLocationRelativeTo(this);
        d.setLayout(new GridLayout(0, 2, 10, 10));
        d.getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField fName  = new JTextField();
        JTextField fEmail = new JTextField();
        d.add(new JLabel("Name:"));  d.add(fName);
        d.add(new JLabel("Email:")); d.add(fEmail);

        JButton ok = btn("Add", new Color(25,135,84));
        ok.addActionListener(e -> {
            String id = "A" + String.format("%03d", attendees.size() + 1);
            attendees.add(new Attendee(id, fName.getText().trim(), fEmail.getText().trim()));
            JOptionPane.showMessageDialog(d, "Attendee added.");
            d.dispose();
        });
        d.add(new JLabel()); d.add(ok);
        d.setVisible(true);
    }

    private void showBookingDialog() {
        if (registry.getEvents().isEmpty() || attendees.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add events and attendees first.");
            return;
        }
        JDialog d = new JDialog(this, "Book Ticket", true);
        d.setSize(400, 260);
        d.setLocationRelativeTo(this);
        d.setLayout(new GridLayout(0, 2, 10, 10));
        d.getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JComboBox<BaseEvent>  fEvent    = new JComboBox<>(registry.getEvents().toArray(new BaseEvent[0]));
        JComboBox<Attendee>   fAttendee = new JComboBox<>(attendees.toArray(new Attendee[0]));
        JTextField fSeats = new JTextField("1");
        JTextField fPrice = new JTextField("500");

        d.add(new JLabel("Event:"));          d.add(fEvent);
        d.add(new JLabel("Attendee:"));       d.add(fAttendee);
        d.add(new JLabel("Seats:"));          d.add(fSeats);
        d.add(new JLabel("Price/seat (₹):")); d.add(fPrice);

        JButton ok = btn("Confirm Booking", new Color(255,140,0));
        ok.addActionListener(e -> {
            try {
                BaseEvent ev = (BaseEvent) fEvent.getSelectedItem();
                Attendee  a  = (Attendee)  fAttendee.getSelectedItem();
                int seats    = Integer.parseInt(fSeats.getText().trim());
                double price = Double.parseDouble(fPrice.getText().trim());
                Ticket t = registry.bookTicket(ev, a, seats, price);
                if (t != null) {
                    refreshEventsTable(); refreshBookingsTable();
                    JOptionPane.showMessageDialog(d, "✅ Booked!\n" + t);
                    d.dispose();
                } else {
                    JOptionPane.showMessageDialog(d, "❌ Not enough seats!", "Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(d, "Enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        d.add(new JLabel()); d.add(ok);
        d.setVisible(true);
    }

    private void showCancelDialog() {
        List<Ticket> active = registry.getAllTickets().stream()
                .filter(t -> "CONFIRMED".equals(t.getStatus())).toList();
        if (active.isEmpty()) { JOptionPane.showMessageDialog(this, "No active tickets."); return; }

        Ticket sel = (Ticket) JOptionPane.showInputDialog(
                this, "Select ticket to cancel:", "Cancel Ticket",
                JOptionPane.QUESTION_MESSAGE, null,
                active.toArray(), active.get(0));

        if (sel != null && JOptionPane.showConfirmDialog(
                this, "Cancel: " + sel + "?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            registry.cancelTicket(sel);
            refreshEventsTable(); refreshBookingsTable();
            JOptionPane.showMessageDialog(this, "Ticket cancelled.");
        }
    }

    // ======================== SERIALIZATION ========================

    private void saveEvents() {
        try {
            registry.serializeToFile("events_data.ser");
            JOptionPane.showMessageDialog(this, "✅ Saved to events_data.ser");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadEvents() {
        try {
            registry.deserializeFromFile("events_data.ser");
            refreshEventsTable(); refreshBookingsTable();
            JOptionPane.showMessageDialog(this, "✅ Loaded successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ======================== CONCURRENT DEMO ========================

    public static void runConcurrentDemo() {
        System.out.println("====== Concurrent Booking Test (ReentrantReadWriteLock) ======");
        EventRegistry reg = new EventRegistry();
        Venue v = new Venue("VT", "Demo Venue", "Demo City", 40);
        ConcertEvent ev = new ConcertEvent("ET01", "Demo Concert", "2025-07-01", v, 40, "Demo Artist");
        reg.addEvent(ev);

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            Attendee a = new Attendee("T" + idx, "User" + idx, "u" + idx + "@mail.com");
            threads.add(new Thread(() -> {
                Ticket t = reg.bookTicket(ev, a, 8, 200);
                System.out.printf("Thread %d → %s%n", idx,
                        t != null ? "Booked " + t.getTicketId() : "FAILED (no seats)");
            }));
        }
        threads.forEach(Thread::start);
        threads.forEach(t -> { try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }});
        System.out.printf("Final: %d booked / %d total%n%n", ev.getBookedSeats(), ev.getTotalSeats());
    }

    // ======================== HELPER ========================

    private JButton btn(String label, Color bg) {
        JButton b = new JButton(label);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}