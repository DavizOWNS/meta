package sk.tuke.mp.example;

import sk.tuke.mp.persistence.PersistenceManager;
import sk.tuke.mp.persistence.ReflectivePersistenceManager;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
        //Connection conn = DriverManager.getConnection("jdbc:derby:orm/test.db;create=true");
        Connection conn = DriverManager.getConnection("jdbc:derby:memory:test.db;create=true");

        PersistenceManager manager = new ReflectivePersistenceManager(conn, Person.class, Department.class);

        manager.initializeDatabase();

        Department development = new Department("Development", "DVLP");
        Department marketing = new Department("Marketing", "MARK");

        Person hrasko = new Person("Janko", "Hrasko", 30);
        hrasko.setDepartment(development);
        Person mrkvicka = new Person("Jozko", "Mrkvicka", 25);
        mrkvicka.setDepartment(development);
        Person novak = new Person("Jan", "Novak", 45);
        novak.setDepartment(marketing);
        Person mrkva = new Person("Janko", "Mrkva", 33);
        mrkva.setDepartment(marketing);

        manager.save(hrasko);
        manager.save(mrkvicka);
        manager.save(novak);
        manager.save(mrkva);

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

        conn.close();
        /*try {
            DriverManager.getConnection("jdbc:derby:orm/test.db;shutdown=true").close();
        }
        catch (Exception ignored){}*/
    }
}
