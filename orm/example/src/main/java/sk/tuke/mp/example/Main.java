package sk.tuke.mp.example;

import sk.tuke.mp.persistence.PersistenceManager;
import sk.tuke.mp.persistence.ReflectivePersistenceManager;
import sk.tuke.mp.persistence.annotations.Transaction;

import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class Main {
    private static Department development;
    private static Department marketing;

    public static void main(String[] args) throws Exception {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
        //Connection conn = DriverManager.getConnection("jdbc:derby:orm/test.db;create=true");
        Connection conn = DriverManager.getConnection("jdbc:derby:memory:test.db;create=true");

        System.out.println("Creating persistence manager");
        PersistenceManager manager = new ReflectivePersistenceManager(conn);

        manager.initializeDatabase();

        CreateDepartments();

        Person hrasko = new Person("Janko", "Hrasko", 30);
        hrasko.setDepartment(development);
        Person mrkvicka = new Person("Jozko", "Mrkvicka", 25);
        mrkvicka.setDepartment(development);
        Person novak = new Person("Jan", "Novak", 45);
        novak.setDepartment(marketing);
        Person mrkva = new Person("Janko", "Mrkva", 33);
        mrkva.setDepartment(marketing);

        //manager.save(hrasko);
        hrasko.setAge(55);
        hrasko.getName();
        //manager.save(hrasko);
        //manager.save(mrkvicka);
        //manager.save(novak);
        //manager.save(mrkva);

        List<IPerson> people = manager.getAll(IPerson.class);
        for (IPerson person : people) {
            System.out.println(person);
            System.out.println("  " + person.getDepartment());
        }

        List<Person> people2 = manager.getBy(Person.class, "surname", "Janko");
        for (Person person : people2) {
            System.out.println(person);
            System.out.println("  " + person.getDepartment());
        }

        IPerson p1 = manager.get(IPerson.class, 1);
        System.out.println("Returned p1");
        p1.getDepartment();

        conn.close();
//        try {
//            DriverManager.getConnection("jdbc:derby:orm/test.db;shutdown=true").close();
//        }
//        catch (Exception ignored){}
    }

    @Transaction
    private static void CreateDepartments()
    {
        development = new Department("Development", "DVLP");
        marketing = new Department("Marketing", "MARK");
    }
}
