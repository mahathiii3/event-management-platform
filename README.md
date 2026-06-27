# 🎟️ Event Management Platform

A comprehensive desktop-based **Event Management Platform** developed in **Java** using **Object-Oriented Programming (OOP)** principles and **Java Swing**. The application enables users to create and manage events, register attendees, book and cancel tickets, visualize attendance statistics, and perform thread-safe concurrent booking operations.

This project demonstrates practical implementation of **Core Java**, **GUI development**, **multithreading**, **serialization**, and **software engineering principles**.

---

## 🚀 Features

* Create and manage multiple event types
* Register and manage attendees
* Book and cancel tickets
* Thread-safe concurrent booking system
* Save and load event data using serialization
* Interactive GUI built with Java Swing
* Attendance visualization using charts
* Color-coded booking status tracking
* Exception handling with user-friendly dialogs
* Concurrent ticket booking simulation

---

## 🛠️ Technologies Used

* Java
* Java Swing
* Object-Oriented Programming (OOP)
* Java Collections Framework
* Multithreading
* Serialization
* Exception Handling
* Graphics2D
* IntelliJ IDEA
* Git & GitHub

---

## 📚 OOP Concepts Implemented

### Inheritance

* `ConcertEvent` and `ConferenceEvent` extend the abstract `BaseEvent` class.

### Abstraction

* `BaseEvent` is implemented as an abstract class to define common event behavior.

### Interfaces

* `Bookable`
* `Cancellable`

### Polymorphism

* Method overriding is used to provide event-specific behavior.

### Encapsulation

* Class data members are protected using access modifiers and controlled through getter/setter methods.

---

## 🔄 Concurrency and Thread Safety

The application demonstrates thread-safe ticket booking using:

```java
ReentrantReadWriteLock
```

Features include:

* Multiple concurrent read operations
* Exclusive write operations
* Prevention of overbooking
* Data consistency during concurrent access

---

## 💾 Data Persistence

The platform uses **Java Object Serialization** to persist application data.

* Save event and attendee information to `.ser` files.
* Restore saved data when the application restarts.

---

## 🖥️ GUI Components Used

* JFrame
* JPanel
* JTable
* JTabbedPane
* JButton
* JComboBox
* JOptionPane
* JScrollPane
* JDialog
* JProgressBar

---

## 📸 Application Screenshots

### Main Events Dashboard

![Main Events Tab](./images/Main%20Events%20Tab%20.png)

### Attendee List

![Attendee List](./images/Attendee%20List%20\(TreeSet\)%20for%20Selected%20Event.png)

### Book Ticket Dialog

![Book Ticket Dialog](./images/Book%20Ticket%20Dialog.png)

### Bookings Dashboard

![Bookings Dashboard](./images/Bookings%20Tab%20%E2%80%94%20Colour-Coded%20Status.png)

### Reports Dashboard

![Reports Dashboard](./images/Reports%20Tab%20.png)

### Concurrent Booking Demonstration

![Concurrency Demo](./images/Console%20Output%20%E2%80%94%20Concurrent%20Booking%20Thread%20Test.png)

### Exception Handling

![Exception Handling](./images/Exception%20Handling%20%E2%80%94%20Insufficient%20Seats%20Error.png)

### Save Operation

![Save Operation](./images/Save%20%E2%80%94%20Serialization.png)

### Load Operation

![Load Operation](./images/Load%20%E2%80%94%20Serialization.png)

---

## ⚙️ Installation and Setup

### Clone the Repository

```bash
git clone https://github.com/mahathiii3/event-management-platform.git
```

### Open in IntelliJ IDEA

1. Open IntelliJ IDEA.
2. Select **Open Project**.
3. Choose the cloned project folder.

### Run the Application

Execute:

```text
Main.java
```

---

## 📁 Project Structure

```text
event-management-platform/
│
├── docs/
│   └── JAVA CASE STUDY REPORT FINAL.docx
│
├── images/
│
├── src/
│   └── eventmanagementapp/
│
├── .idea/
├── .gitignore
├── EventManagementApp.iml
├── events_data.ser
└── README.md
```

---

## 🔮 Future Enhancements

* Database integration using MySQL
* User authentication and authorization
* Search and filtering functionality
* Payment gateway integration
* Report export as PDF/Excel
* Cloud-based storage support

---

## 🎯 Learning Outcomes

This project strengthened practical understanding of:

* Core Java Programming
* Object-Oriented Design
* Java Swing GUI Development
* Multithreading and Synchronization
* Collections Framework
* File Handling and Serialization
* Exception Handling
* Software Engineering Principles

---

## 👩‍💻 Author

**Mahathi Vaka**

B.Tech Computer Science and Business Systems
SRM Institute of Science and Technology, Kattankulathur

---

⭐ If you found this project interesting, consider giving it a star!
