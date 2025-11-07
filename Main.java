import java.util.*;

/**
 * HashAndAVLPlayground
 * A tiny, fun console game that uses a simple hash-based set to track/validate words
 * and keeps a high-score leaderboard in an AVL self-balancing binary search tree.
 *
 * How it works
 * - You get 5 scrambled-word rounds.
 * - Exact guess of the original word: +10 points
 * - A different but valid word from our mini-dictionary: +5 points
 * - Otherwise: 0 points
 * - After playing, your score is inserted into an AVL tree and we show a top-5 leaderboard.
 *
 */
public class Main {

    // ======== Simple HashSet (chaining) ========
    static class SimpleHashSet {
        static class Node {
            String key;
            Node next;
            Node(String k, Node n) { key = k; next = n; }
        }

        private Node[] buckets;
        private int capacity;
        private int size;

        public SimpleHashSet(int capacity) {
            this.capacity = Math.max(17, capacity);
            this.buckets = new Node[this.capacity];
            this.size = 0;
        }

        // Simple djb2-style hash (compact, decent distribution for demo)
        private int hash(String s) {
            long h = 5381;
            for (int i = 0; i < s.length(); i++) {
                h = ((h << 5) + h) + s.charAt(i); // h * 33 + c
            }
            if (h < 0) h = -h;
            return (int)(h % capacity);
        }

        public boolean contains(String key) {
            int idx = hash(key);
            Node cur = buckets[idx];
            while (cur != null) {
                if (cur.key.equals(key)) return true;
                cur = cur.next;
            }
            return false;
        }

        public void add(String key) {
            int idx = hash(key);
            Node cur = buckets[idx];
            while (cur != null) {
                if (cur.key.equals(key)) return; // already there
                cur = cur.next;
            }
            buckets[idx] = new Node(key, buckets[idx]);
            size++;
        }

        public int size() { return size; }
    }

    // ======== AVL Tree for Leaderboard ========
    static class PlayerScore {
        String name;
        int score;
        PlayerScore(String n, int s) { name = n; score = s; }
        @Override public String toString() { return name + " (" + score + ")"; }
    }

    static class AVLTree {
        static class Node {
            PlayerScore val;
            Node left, right;
            int height;
            Node(PlayerScore v) { val = v; height = 1; }
        }

        private Node root;

        // Compare by score, then name to break ties
        private int compare(PlayerScore a, PlayerScore b) {
            if (a.score != b.score) return Integer.compare(a.score, b.score);
            return a.name.compareToIgnoreCase(b.name);
        }

        private int height(Node n) { return n == null ? 0 : n.height; }
        private int balanceFactor(Node n) { return n == null ? 0 : height(n.left) - height(n.right); }
        private void update(Node n) { n.height = 1 + Math.max(height(n.left), height(n.right)); }

        private Node rotateRight(Node y) {
            Node x = y.left;
            Node T2 = x.right;
            x.right = y;
            y.left = T2;
            update(y); update(x);
            return x;
        }
        private Node rotateLeft(Node x) {
            Node y = x.right;
            Node T2 = y.left;
            y.left = x;
            x.right = T2;
            update(x); update(y);
            return y;
        }

        public void insert(PlayerScore val) { root = insert(root, val); }
        private Node insert(Node node, PlayerScore val) {
            if (node == null) return new Node(val);

            int cmp = compare(val, node.val);
            if (cmp < 0) node.left = insert(node.left, val);
            else if (cmp > 0) node.right = insert(node.right, val);
            else {
                // same (score, name) -> keep one; you could choose to update name/score here
                return node;
            }

            update(node);
            int bf = balanceFactor(node);

            // LL
            if (bf > 1 && compare(val, node.left.val) < 0)
                return rotateRight(node);
            // RR
            if (bf < -1 && compare(val, node.right.val) > 0)
                return rotateLeft(node);
            // LR
            if (bf > 1 && compare(val, node.left.val) > 0) {
                node.left = rotateLeft(node.left);
                return rotateRight(node);
            }
            // RL
            if (bf < -1 && compare(val, node.right.val) < 0) {
                node.right = rotateRight(node.right);
                return rotateLeft(node);
            }
            return node;
        }

        // Reverse in-order to get scores from high to low
        public void topK(int k, List<PlayerScore> out) { topK(root, k, out); }
        private void topK(Node node, int k, List<PlayerScore> out) {
            if (node == null || out.size() >= k) return;
            topK(node.right, k, out);
            if (out.size() < k) out.add(node.val);
            topK(node.left, k, out);
        }
    }

    // ======== Word Scramble Game ========
    private static final String[] WORDS = {
        "orange","puzzle","stream","planet","binary","silent","listen","triangle",
        "castle","python","java","random","bubble","forest","rocket","galaxy"
    };

    private static String scramble(String word, Random rng) {
        char[] arr = word.toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
        String s = new String(arr);
        // avoid the rare case it matches the original
        if (s.equals(word) && word.length() > 1) return scramble(word, rng);
        return s;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Random rng = new Random(System.nanoTime());

        System.out.println("==== Welcome to Hash & AVL Playground ====\n");
        System.out.print("Your name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) name = "Player" + (1 + rng.nextInt(999));

        // Build a mini dictionary using our SimpleHashSet
        SimpleHashSet dict = new SimpleHashSet(53);
        for (String w : WORDS) dict.add(w);

        // Play 5 rounds
        int rounds = 5;
        int score = 0;
        boolean[] used = new boolean[WORDS.length];
        System.out.println("\nYou will get " + rounds + " scrambled words.\nType your guess and press Enter.\nExact match: +10, Valid different word: +5\n");

        for (int r = 1; r <= rounds; r++) {
            int idx;
            do { idx = rng.nextInt(WORDS.length); } while (used[idx]);
            used[idx] = true;
            String word = WORDS[idx];
            String scrambled = scramble(word, rng);

            System.out.println("Round " + r + ": " + scrambled);
            System.out.print("Your guess: ");
            String guess = sc.nextLine().trim().toLowerCase();

            if (guess.equals(word)) {
                score += 10;
                System.out.println("Correct! +10 points\n");
            } else if (dict.contains(guess)) {
                score += 5;
                System.out.println("That's a valid word from the dictionary, but not the hidden one. +5 points\n");
            } else {
                System.out.println("Not in the mini-dictionary. 0 points. The word was: " + word + "\n");
            }
        }

        System.out.println("Game over, " + name + "! Your score: " + score + "\n");

        // Build an AVL leaderboard and show Top-5
        AVLTree leaderboard = new AVLTree();
       
        // Add current player
        leaderboard.insert(new PlayerScore(name, score));

        List<PlayerScore> top = new ArrayList<>();
        leaderboard.topK(5, top);
        System.out.println("===== Leaderboard (Top 5) =====");
        int rank = 1;
        for (PlayerScore ps : top) {
            System.out.printf("%d. %s\n", rank++, ps);
        }

        // Tiny peek: show hash bucket count used by our dictionary
        System.out.println("\n(HashSet size: " + dict.size() + ") \n");
        System.out.println("Thanks for playing");
    }
}