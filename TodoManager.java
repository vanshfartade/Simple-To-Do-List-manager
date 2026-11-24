import java.util.HashMap;
import java.util.Scanner;

/**
 * Simple To-Do List Manager - DSA Mini Project
 *
 * Data structures used (implemented from scratch where it matters):
 *  1. Singly Linked List  -> stores all tasks in insertion order
 *  2. HashMap             -> O(1) lookup of a task by its ID
 *  3. Min-Heap            -> shows tasks sorted by priority (heap sort idea)
 *  4. Stack (array-based) -> UNDO feature (LIFO)
 */
public class TodoManager {

    // ---------------------------------------------------------------
    // Task = node of the linked list
    // ---------------------------------------------------------------
    static class Task {
        int id;
        String title;
        int priority;      // 1 = High, 2 = Medium, 3 = Low
        boolean done;
        Task next;         // link to next node

        Task(int id, String title, int priority) {
            this.id = id;
            this.title = title;
            this.priority = priority;
        }

        String priorityName() {
            return priority == 1 ? "High" : priority == 2 ? "Medium" : "Low";
        }

        @Override
        public String toString() {
            return String.format("[%d] %-25s | %-6s | %s",
                    id, title, priorityName(), done ? "Done" : "Pending");
        }
    }

    // ---------------------------------------------------------------
    // 1. Singly Linked List
    // ---------------------------------------------------------------
    static class TaskList {
        Task head, tail;
        int size = 0;

        // O(1)
        void addLast(Task t) {
            t.next = null;
            if (head == null) head = tail = t;
            else { tail.next = t; tail = t; }
            size++;
        }

        // O(n)
        Task remove(int id) {
            Task prev = null, cur = head;
            while (cur != null) {
                if (cur.id == id) {
                    if (prev == null) head = cur.next;
                    else prev.next = cur.next;
                    if (cur == tail) tail = prev;
                    cur.next = null;
                    size--;
                    return cur;
                }
                prev = cur;
                cur = cur.next;
            }
            return null;
        }

        // O(n)
        void display() {
            if (head == null) { System.out.println("  (no tasks)"); return; }
            for (Task t = head; t != null; t = t.next) System.out.println("  " + t);
        }
    }

    // ---------------------------------------------------------------
    // 3. Min-Heap (ordered by priority, then by id)
    // ---------------------------------------------------------------
    static class MinHeap {
        Task[] arr;
        int n = 0;

        MinHeap(int capacity) { arr = new Task[Math.max(capacity, 1)]; }

        boolean less(Task a, Task b) {
            return a.priority < b.priority || (a.priority == b.priority && a.id < b.id);
        }

        // O(log n)
        void insert(Task t) {
            arr[n] = t;
            int i = n++;
            while (i > 0 && less(arr[i], arr[(i - 1) / 2])) {
                swap(i, (i - 1) / 2);
                i = (i - 1) / 2;
            }
        }

        // O(log n)
        Task extractMin() {
            Task min = arr[0];
            arr[0] = arr[--n];
            int i = 0;
            while (true) {
                int l = 2 * i + 1, r = 2 * i + 2, s = i;
                if (l < n && less(arr[l], arr[s])) s = l;
                if (r < n && less(arr[r], arr[s])) s = r;
                if (s == i) break;
                swap(i, s);
                i = s;
            }
            return min;
        }

        void swap(int a, int b) { Task t = arr[a]; arr[a] = arr[b]; arr[b] = t; }
        boolean isEmpty() { return n == 0; }
    }

    // ---------------------------------------------------------------
    // 4. Stack for Undo
    // ---------------------------------------------------------------
    static final int ADD = 1, DELETE = 2, COMPLETE = 3;

    static class Action {
        int type;
        Task task;
        Action(int type, Task task) { this.type = type; this.task = task; }
    }

    static class UndoStack {
        Action[] data = new Action[10];
        int top = -1;

        void push(Action a) {
            if (top == data.length - 1) {              // grow array when full
                Action[] bigger = new Action[data.length * 2];
                System.arraycopy(data, 0, bigger, 0, data.length);
                data = bigger;
            }
            data[++top] = a;
        }

        Action pop() { return top == -1 ? null : data[top--]; }
        boolean isEmpty() { return top == -1; }
    }

    // ---------------------------------------------------------------
    // Manager (ties everything together)
    // ---------------------------------------------------------------
    static TaskList list = new TaskList();
    static HashMap<Integer, Task> index = new HashMap<>();
    static UndoStack undo = new UndoStack();
    static int nextId = 1;

    static void addTask(String title, int priority) {
        Task t = new Task(nextId++, title, priority);
        list.addLast(t);
        index.put(t.id, t);
        undo.push(new Action(ADD, t));
        System.out.println("Added: " + t);
    }

    static void viewAll() {
        System.out.println("\n--- All Tasks (in order added) ---");
        list.display();
    }

    static void viewByPriority() {
        System.out.println("\n--- Pending Tasks by Priority (Min-Heap) ---");
        MinHeap heap = new MinHeap(list.size);
        for (Task t = list.head; t != null; t = t.next)
            if (!t.done) heap.insert(t);
        if (heap.isEmpty()) { System.out.println("  (no pending tasks)"); return; }
        while (!heap.isEmpty()) System.out.println("  " + heap.extractMin());
    }

    static void completeTask(int id) {
        Task t = index.get(id);                         // O(1) lookup
        if (t == null) { System.out.println("No task with ID " + id); return; }
        if (t.done) { System.out.println("Task already completed."); return; }
        t.done = true;
        undo.push(new Action(COMPLETE, t));
        System.out.println("Completed: " + t);
    }

    static void deleteTask(int id) {
        Task t = list.remove(id);
        if (t == null) { System.out.println("No task with ID " + id); return; }
        index.remove(id);
        undo.push(new Action(DELETE, t));
        System.out.println("Deleted: " + t.title);
    }

    static void searchTask(int id) {
        Task t = index.get(id);
        System.out.println(t == null ? "Task not found." : "Found: " + t);
    }

    static void undoLast() {
        Action a = undo.pop();
        if (a == null) { System.out.println("Nothing to undo."); return; }
        switch (a.type) {
            case ADD:                                   // undo add -> remove
                list.remove(a.task.id);
                index.remove(a.task.id);
                System.out.println("Undo: removed '" + a.task.title + "'");
                break;
            case DELETE:                                // undo delete -> put back
                list.addLast(a.task);
                index.put(a.task.id, a.task);
                System.out.println("Undo: restored '" + a.task.title + "'");
                break;
            case COMPLETE:                              // undo complete -> pending
                a.task.done = false;
                System.out.println("Undo: '" + a.task.title + "' is pending again");
                break;
        }
    }

    // ---------------------------------------------------------------
    // Console menu
    // ---------------------------------------------------------------
    static int readInt(Scanner sc, String msg) {
        while (true) {
            System.out.print(msg);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int choice;
        do {
            System.out.println("\n===== TO-DO LIST MANAGER =====");
            System.out.println("1. Add task");
            System.out.println("2. View all tasks");
            System.out.println("3. View pending tasks by priority");
            System.out.println("4. Mark task as completed");
            System.out.println("5. Delete task");
            System.out.println("6. Search task by ID");
            System.out.println("7. Undo last action");
            System.out.println("0. Exit");
            choice = readInt(sc, "Choose: ");

            switch (choice) {
                case 1:
                    System.out.print("Title: ");
                    String title = sc.nextLine().trim();
                    if (title.isEmpty()) { System.out.println("Title cannot be empty."); break; }
                    int p;
                    do { p = readInt(sc, "Priority (1=High, 2=Medium, 3=Low): "); } while (p < 1 || p > 3);
                    addTask(title, p);
                    break;
                case 2: viewAll(); break;
                case 3: viewByPriority(); break;
                case 4: completeTask(readInt(sc, "Task ID: ")); break;
                case 5: deleteTask(readInt(sc, "Task ID: ")); break;
                case 6: searchTask(readInt(sc, "Task ID: ")); break;
                case 7: undoLast(); break;
                case 0: System.out.println("Goodbye!"); break;
                default: System.out.println("Invalid choice.");
            }
        } while (choice != 0);
        sc.close();
    }
}