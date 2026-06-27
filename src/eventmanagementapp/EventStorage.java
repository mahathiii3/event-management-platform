package eventmanagementapp;

import java.io.*;
import java.util.List;

public class EventStorage {

    public static void save(EventRegistry registry, String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(registry.getEvents());
            oos.writeObject(registry.getAllTickets());
        }
    }

    @SuppressWarnings("unchecked")
    public static void load(EventRegistry registry, String filename)
            throws IOException, ClassNotFoundException {

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {

            List<BaseEvent> events = (List<BaseEvent>) ois.readObject();
            List<Ticket> tickets = (List<Ticket>) ois.readObject();

            registry.getEvents().clear();
            registry.getAllTickets().clear();

            registry.getEvents().addAll(events);
            registry.getAllTickets().addAll(tickets);
        }
    }
}