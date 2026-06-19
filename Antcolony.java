import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class IACO_Final_GUI extends JFrame {

    static final Color C_BG       = new Color(0xF4F4F2);
    static final Color C_CARD     = Color.WHITE;
    static final Color C_TOPBAR   = new Color(0x185FA5);
    static final Color C_IACO     = new Color(0x185FA5);   // blue
    static final Color C_ACO      = new Color(0x639922);   // green
    static final Color C_NODE     = new Color(0x2E4057);
    static final Color C_NODE_FG  = Color.WHITE;
    static final Color C_EDGE     = new Color(0xDDDCDA);
    static final Color C_PATH     = new Color(0xFF5733);
    static final Color C_ACCENT   = new Color(0x185FA5);
    static final Color C_MUTED    = new Color(0x888780);
    static final Color C_BORDER   = new Color(0xD3D1C7);
    static final Color C_START    = new Color(0x185FA5);
    static final Color C_MET_IACO = new Color(0xEAF3DE);
    static final Color C_MET_ACO  = new Color(0xFAECE7);
    static final Color C_MET_TXT_I= new Color(0x3B6D11);
    static final Color C_MET_TXT_A= new Color(0x712B13);

    private NetworkPanel   networkPanel;
    private ConvergPanel   convPanel;
    private PheromonePanel pheroPanel;

    private JLabel lblIBest, lblIAvg, lblIIter, lblITime;
    private JLabel lblABest, lblAAvg, lblAIter, lblATime;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    private JSpinner spnNodes, spnAnts, spnIter;
    private JSlider  sldAlpha, sldBeta, sldRho, sldQ0, sldMut, sldTopL;
    private JLabel   valAlpha, valBeta, valRho, valQ0, valMut, valTopL;

    private JButton btnBoth, btnIACO, btnACO, btnStop, btnReset, btnNew;

    private volatile boolean stopFlag = false;
    private Random rand = new Random();

    // current problem
    private int N = 15;
    private double[][] dist;
    private double[][] nodeX, nodeY;   // (not matrices – kept as flat arrays below)
    private double[] px, py;          // 2-D positions for drawing

    // results
    private List<Double> iacoHistory = new ArrayList<>();
    private List<Double> acoHistory  = new ArrayList<>();
    private List<Integer> iacoBestPath = new ArrayList<>();
    private List<Integer> acoBestPath  = new ArrayList<>();
    private double[][] iaco_pheromone;

    public IACO_Final_GUI() {
        super("IACO Simulator  —  CS6L Algorithms & Complexity");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1360, 860);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);

        buildUI();
        newProblem();
        setVisible(true);
    }


    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildBody(),      BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // top bar 
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        bar.setBackground(C_TOPBAR);
        bar.setPreferredSize(new Dimension(0, 48));

        JLabel title = new JLabel("  IACO Simulator");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(Color.WHITE);
        bar.add(title);

        JLabel sub = new JLabel("Improved Ant Colony Optimization  •  Yang & Zhuang (2010)");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        sub.setForeground(new Color(0xB5D4F4));
        bar.add(sub);
        return bar;
    }

    //  body
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(8, 0));
        body.setBackground(C_BG);
        body.setBorder(new EmptyBorder(8, 10, 0, 10));

        body.add(buildParamPanel(), BorderLayout.WEST);
        body.add(buildCenter(),     BorderLayout.CENTER);
        body.add(buildMetrics(),    BorderLayout.EAST);
        return body;
    }

    // parameter panel 
    private JPanel buildParamPanel() {
        JPanel card = card(240, 0);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        addHdr(card, "Parameters");

        addSec(card, "Problem");
        spnNodes = addSpinner(card, "Nodes 👎",      5,  40, N,    1);
        spnAnts  = addSpinner(card, "Ants (m)",        3,  30, 20,   1);
        spnIter  = addSpinner(card, "Max iterations", 50, 800, 200, 10);

        addSec(card, "Pheromone");
        Object[] rho = addSlider(card, "Evaporation ρ",  1, 90, 10);
        sldRho = (JSlider) rho[0]; valRho = (JLabel) rho[1];

        Object[] topL = addSlider(card, "Top-l ants",     1, 10,  3);
        sldTopL = (JSlider) topL[0]; valTopL = (JLabel) topL[1];

        addSec(card, "Selection");
        Object[] alpha = addSlider(card, "Alpha α",      10, 30, 10);
        sldAlpha = (JSlider) alpha[0]; valAlpha = (JLabel) alpha[1];

        Object[] beta = addSlider(card, "Beta β",        10, 50, 20);
        sldBeta = (JSlider) beta[0]; valBeta = (JLabel) beta[1];

        Object[] q0 = addSlider(card, "Pseudo-rand q₀", 10, 99, 90);
        sldQ0 = (JSlider) q0[0]; valQ0 = (JLabel) q0[1];

        addSec(card, "Genetic operator (IACO)");
        Object[] mut = addSlider(card, "Mutation prob.", 0, 100, 50);
        sldMut = (JSlider) mut[0]; valMut = (JLabel) mut[1];

        card.add(Box.createVerticalGlue());

        sep(card);
        btnNew = styledBtn("⟳  New Problem", C_BG, Color.DARK_GRAY);
        btnNew.addActionListener(e -> newProblem());
        card.add(btnNew);

        return card;
    }

    // center notebook (tabs)
    private JPanel buildCenter() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(C_BG);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        networkPanel = new NetworkPanel();
        convPanel    = new ConvergPanel();
        pheroPanel   = new PheromonePanel();

        tabs.addTab("   Network Graph   ", networkPanel);
        tabs.addTab("   Convergence Curve   ", convPanel);
        tabs.addTab("   Pheromone Map   ", pheroPanel);

        wrap.add(tabs, BorderLayout.CENTER);
        return wrap;
    }

    //  metrics panel 
    private JPanel buildMetrics() {
        JPanel card = card(230, 0);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 12, 14, 12));

        addHdr(card, "Results");

        addSec(card, "IACO  (improved)");
        lblIBest = metCard(card, "Best path cost",      C_MET_IACO, C_MET_TXT_I);
        lblIAvg  = metCard(card, "Avg cost (last 20)",  C_MET_IACO, C_MET_TXT_I);
        lblIIter = metCard(card, "Convergence @ iter",  C_MET_IACO, C_MET_TXT_I);
        lblITime = metCard(card, "Runtime",             C_MET_IACO, C_MET_TXT_I);

        addSec(card, "Baseline ACO");
        lblABest = metCard(card, "Best path cost",      C_MET_ACO,  C_MET_TXT_A);
        lblAAvg  = metCard(card, "Avg cost (last 20)",  C_MET_ACO,  C_MET_TXT_A);
        lblAIter = metCard(card, "Convergence @ iter",  C_MET_ACO,  C_MET_TXT_A);
        lblATime = metCard(card, "Runtime",             C_MET_ACO,  C_MET_TXT_A);

        addSec(card, "Activity log");
        JTextArea log = new JTextArea(8, 18);
        log.setFont(new Font("Consolas", Font.PLAIN, 9));
        log.setBackground(C_BG);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(log);
        sp.setBorder(BorderFactory.createLineBorder(C_BORDER));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        card.add(sp);

        this.logArea = log;

        card.add(Box.createVerticalGlue());
        return card;
    }
    private JTextArea logArea;

    //  status bar 
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(new Color(0x2C2C2A));
        bar.setPreferredSize(new Dimension(0, 52));

        btnBoth  = barBtn("▶  Run Both",  C_TOPBAR);
        btnIACO  = barBtn("▶  IACO only", new Color(0x1D9E75));
        btnACO   = barBtn("▶  ACO only",  new Color(0x639922));
        btnStop  = barBtn("⬛  Stop",      new Color(0x993C1D));
        btnReset = barBtn("↺  Reset",     new Color(0x5F5E5A));

        btnBoth .addActionListener(e -> runBoth());
        btnIACO .addActionListener(e -> runAlg(true,  null));
        btnACO  .addActionListener(e -> runAlg(false, null));
        btnStop .addActionListener(e -> { stopFlag = true; setStatus("Stopped."); });
        btnReset.addActionListener(e -> resetAll());
        btnStop .setEnabled(false);

        bar.add(btnBoth); bar.add(btnIACO); bar.add(btnACO);
        bar.add(btnStop); bar.add(btnReset);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(180, 14));
        progressBar.setStringPainted(false);
        bar.add(Box.createHorizontalStrut(20));
        bar.add(progressBar);

        statusLabel = new JLabel("Ready — click Run Both to compare algorithms");
        statusLabel.setForeground(new Color(0xB4B2A9));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        bar.add(Box.createHorizontalStrut(12));
        bar.add(statusLabel);
        return bar;
    }

    private void newProblem() {
        N  = (Integer) spnNodes.getValue();
        dist = new double[N][N];
        px   = new double[N];
        py   = new double[N];

        // random symmetric distance matrix
        for (int i = 0; i < N; i++)
            for (int j = i + 1; j < N; j++) {
                double d = rand.nextInt(90) + 10;
                dist[i][j] = d;
                dist[j][i] = d;
            }

        // positions on a circle + small jitter
        for (int i = 0; i < N; i++) {
            double angle = 2 * Math.PI * i / N;
            px[i] = 0.75 * Math.cos(angle) + (rand.nextDouble() - 0.5) * 0.15;
            py[i] = 0.75 * Math.sin(angle) + (rand.nextDouble() - 0.5) * 0.15;
        }

        resetAll();
        log("New problem: " + N + " nodes, " + (N*(N-1)/2) + " edges.");
        setStatus("Problem ready — " + N + " nodes.");
    }

    /** Run both ACO then IACO sequentially on worker thread. */
    private void runBoth() {
        runAlg(false, () -> runAlg(true, null));
    }

    private void runAlg(boolean useIACO, Runnable onDone) {
        stopFlag = false;
        setRunning(true);
        String label = useIACO ? "IACO" : "ACO";
        setStatus("Running " + label + "…");

        int    m    = (Integer) spnAnts.getValue();
        int    iter = (Integer) spnIter.getValue();
        double alpha = sldAlpha.getValue() / 10.0;
        double beta  = sldBeta .getValue() / 10.0;
        double rho   = sldRho  .getValue() / 100.0;
        double q0    = sldQ0   .getValue() / 100.0;
        double mutP  = sldMut  .getValue() / 100.0;
        int    topL  = sldTopL .getValue();

        new Thread(() -> {
            double[][] pheromone = new double[N][N];
            double tau0 = 0.1;
            for (int i = 0; i < N; i++)
                Arrays.fill(pheromone[i], tau0);

            List<Double>  history  = new ArrayList<>();
            List<Integer> bestPath = new ArrayList<>();
            double bestCost = Double.MAX_VALUE;
            long t0 = System.currentTimeMillis();

            for (int it = 0; it < iter && !stopFlag; it++) {
                // construct tours
                List<List<Integer>> tours = new ArrayList<>();
                List<Double>        costs = new ArrayList<>();

                for (int k = 0; k < m; k++) {
                    List<Integer> tour = constructTour(pheromone, alpha, beta, q0,
                                                       useIACO, tau0);
                    double cost = tourCost(tour);
                    tours.add(tour);
                    costs.add(cost);
                }

                // sort by cost
                Integer[] idx = new Integer[m];
                for (int k = 0; k < m; k++) idx[k] = k;
                Arrays.sort(idx, Comparator.comparingDouble(costs::get));

                // track global best
                double iterBest = costs.get(idx[0]);
                if (iterBest < bestCost) {
                    bestCost = iterBest;
                    bestPath = new ArrayList<>(tours.get(idx[0]));
                }

                // IACO mutation: try swapping two nodes in best tour
                if (useIACO && rand.nextDouble() < mutP && bestPath.size() > 3) {
                    List<Integer> mutated = mutate(bestPath);
                    double mc = tourCost(mutated);
                    if (mc < bestCost) {
                        bestCost = mc;
                        bestPath = mutated;
                    }
                }

                // pheromone evaporation
                for (int i = 0; i < N; i++)
                    for (int j = 0; j < N; j++)
                        pheromone[i][j] *= (1 - rho);

                // pheromone deposit
                if (useIACO) {
                    // IACO: top-l ants deposit, rank-weighted (eq. 9)
                    int L = Math.min(topL, m);
                    for (int rank = 0; rank < L; rank++) {
                        List<Integer> t = tours.get(idx[rank]);
                        double contrib  = rho / ((rank + 1) * costs.get(idx[rank]));
                        for (int s = 0; s < t.size() - 1; s++) {
                            int a = t.get(s), b = t.get(s + 1);
                            pheromone[a][b] += contrib;
                            pheromone[b][a] += contrib;
                        }
                    }
                } else {
                    // ACO baseline: only the single best ant deposits
                    List<Integer> bt = tours.get(idx[0]);
                    double contrib = rho / costs.get(idx[0]);
                    for (int s = 0; s < bt.size() - 1; s++) {
                        int a = bt.get(s), b = bt.get(s + 1);
                        pheromone[a][b] += contrib;
                        pheromone[b][a] += contrib;
                    }
                }

                history.add(bestCost);

                // live update every ~10 iterations
                final int itFinal = it;
                final double costSnap = bestCost;
                final List<Integer> pathSnap = new ArrayList<>(bestPath);
                final double[][] phSnap = deepCopy(pheromone);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue((int)((itFinal + 1) * 100.0 / iter));
                    networkPanel.update(pathSnap, phSnap, useIACO);
                    if (useIACO) convPanel.setIACO(history);
                    else         convPanel.setACO(history);
                    pheroPanel.update(phSnap);
                    setStatus(label + " iter " + (itFinal+1) + "/" + iter
                              + "  best: " + String.format("%.2f", costSnap));
                });

                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }

            long elapsed = System.currentTimeMillis() - t0;
            double finalCost = bestCost;
            List<Double> finalHist = history;
            List<Integer> finalPath = bestPath;
            double[][] finalPh = pheromone;

            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(100);
                networkPanel.update(finalPath, finalPh, useIACO);
                pheroPanel.update(finalPh);

                // compute metrics
                double avg = finalHist.isEmpty() ? 0 :
                    finalHist.subList(Math.max(0, finalHist.size() - 20), finalHist.size())
                             .stream().mapToDouble(Double::doubleValue).average().orElse(0);
                int convIter = 0;
                for (int i = 0; i < finalHist.size(); i++)
                    if (finalHist.get(i) == finalCost) { convIter = i; break; }

                String costStr = String.format("%.2f", finalCost);
                String avgStr  = String.format("%.2f", avg);
                String timeStr = (elapsed / 1000.0) + " s";

                if (useIACO) {
                    iacoHistory = new ArrayList<>(finalHist);
                    iacoBestPath = new ArrayList<>(finalPath);
                    iaco_pheromone = finalPh;
                    lblIBest.setText(costStr); lblIAvg.setText(avgStr);
                    lblIIter.setText(String.valueOf(convIter));
                    lblITime.setText(timeStr);
                    convPanel.setIACO(iacoHistory);
                } else {
                    acoHistory = new ArrayList<>(finalHist);
                    acoBestPath = new ArrayList<>(finalPath);
                    lblABest.setText(costStr); lblAAvg.setText(avgStr);
                    lblAIter.setText(String.valueOf(convIter));
                    lblATime.setText(timeStr);
                    convPanel.setACO(acoHistory);
                }

                setRunning(false);
                log(label + " done — best: " + costStr + " | iter: " + convIter
                    + " | " + timeStr);
                setStatus(label + " complete — best cost: " + costStr);

                if (onDone != null) onDone.run();
            });
        }).start();
    }

    //  tour construction (ACS pseudo-random-proportional rule) 
    private List<Integer> constructTour(double[][] ph, double alpha, double beta,
                                        double q0, boolean iaco, double tau0) {
        boolean[] visited = new boolean[N];
        List<Integer> tour = new ArrayList<>();
        int cur = rand.nextInt(N);
        tour.add(cur);
        visited[cur] = true;

        for (int step = 1; step < N; step++) {
            int nxt;
            if (rand.nextDouble() <= q0) {
                // exploitation
                nxt = -1;
                double best = -1;
                for (int j = 0; j < N; j++) {
                    if (!visited[j]) {
                        double v = Math.pow(ph[cur][j], alpha)
                                 * Math.pow(1.0 / dist[cur][j], beta);
                        if (v > best) { best = v; nxt = j; }
                    }
                }
            } else {
                // exploration – roulette
                double[] prob = new double[N];
                double sum = 0;
                for (int j = 0; j < N; j++) {
                    if (!visited[j]) {
                        prob[j] = Math.pow(ph[cur][j], alpha)
                                * Math.pow(1.0 / dist[cur][j], beta);
                        sum += prob[j];
                    }
                }
                double r = rand.nextDouble() * sum, cum = 0;
                nxt = 0;
                for (int j = 0; j < N; j++) {
                    if (!visited[j]) {
                        cum += prob[j];
                        if (cum >= r) { nxt = j; break; }
                    }
                }
            }

            // local pheromone update (IACO)
            if (iaco) {
                ph[cur][nxt] = (1 - 0.1) * ph[cur][nxt] + 0.1 * tau0;
                ph[nxt][cur] = ph[cur][nxt];
            }

            tour.add(nxt);
            visited[nxt] = true;
            cur = nxt;
        }
        tour.add(tour.get(0));   // close tour
        return tour;
    }

    /** IACO mutation: swap two inner nodes if it reduces cost. */
    private List<Integer> mutate(List<Integer> tour) {
        List<Integer> inner = new ArrayList<>(tour.subList(1, tour.size() - 1));
        int k = inner.size();
        if (k < 2) return tour;
        double bestCost = tourCost(tour);
        List<Integer> best = tour;
        for (int a = 0; a < k; a++) {
            for (int b = a + 1; b < k; b++) {
                List<Integer> candidate = new ArrayList<>(tour);
                Collections.swap(candidate, a + 1, b + 1);
                double c = tourCost(candidate);
                if (c < bestCost) { bestCost = c; best = candidate; }
            }
        }
        return best;
    }

    private double tourCost(List<Integer> tour) {
        double cost = 0;
        for (int i = 0; i < tour.size() - 1; i++)
            cost += dist[tour.get(i)][tour.get(i + 1)];
        return cost;
    }

    private double[][] deepCopy(double[][] m) {
        double[][] c = new double[m.length][m[0].length];
        for (int i = 0; i < m.length; i++) c[i] = m[i].clone();
        return c;
    }

    private void resetAll() {
        iacoHistory.clear(); acoHistory.clear();
        iacoBestPath.clear(); acoBestPath.clear();
        progressBar.setValue(0);
        for (JLabel l : new JLabel[]{lblIBest,lblIAvg,lblIIter,lblITime,
                                      lblABest,lblAAvg,lblAIter,lblATime})
            if (l != null) l.setText("—");
        convPanel.reset();
        pheroPanel.reset();
        networkPanel.reset();
        setStatus("Reset.");
    }

    private void setRunning(boolean running) {
        btnBoth .setEnabled(!running);
        btnIACO .setEnabled(!running);
        btnACO  .setEnabled(!running);
        btnReset.setEnabled(!running);
        btnStop .setEnabled(running);
        btnNew  .setEnabled(!running);
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private void log(String msg) {
        logArea.append("› " + msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // widget factories

    private JPanel card(int w, int h) {
        JPanel p = new JPanel();
        p.setBackground(C_CARD);
        p.setBorder(BorderFactory.createLineBorder(C_BORDER));
        if (w > 0) p.setPreferredSize(new Dimension(w, h == 0 ? Integer.MAX_VALUE : h));
        return p;
    }

    private void addHdr(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(0x2C2C2A));
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l); p.add(Box.createVerticalStrut(6));
    }

    private void addSec(JPanel p, String text) {
        p.add(Box.createVerticalStrut(10));
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 8));
        l.setForeground(C_MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(C_BORDER);
        p.add(sep);
        p.add(Box.createVerticalStrut(4));
    }

    private void sep(JPanel p) {
        p.add(Box.createVerticalStrut(8));
        JSeparator s = new JSeparator();
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        p.add(s);
        p.add(Box.createVerticalStrut(8));
    }

    private JSpinner addSpinner(JPanel p, String label, int lo, int hi, int init, int step) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(C_CARD);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        lbl.setForeground(new Color(0x444441));
        JSpinner sp = new JSpinner(new SpinnerNumberModel(init, lo, hi, step));
        sp.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setColumns(4);
        row.add(lbl, BorderLayout.WEST);
        row.add(sp,  BorderLayout.EAST);
        p.add(row);
        p.add(Box.createVerticalStrut(3));
        return sp;
    }

    private Object[] addSlider(JPanel p, String label, int lo, int hi, int init) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(C_CARD);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        lbl.setForeground(new Color(0x444441));

        JLabel val = new JLabel(String.format("%.2f", init / 100.0));
        val.setFont(new Font("Segoe UI", Font.BOLD, 9));
        val.setForeground(C_ACCENT);
        val.setPreferredSize(new Dimension(34, 16));

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        p.add(row);

        JSlider sl = new JSlider(lo, hi, init);
        sl.setBackground(C_CARD);
        sl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        sl.setAlignmentX(LEFT_ALIGNMENT);
        sl.addChangeListener(e -> val.setText(String.format("%.2f", sl.getValue() / 100.0)));
        p.add(sl);
        p.add(Box.createVerticalStrut(2));
        return new Object[]{sl, val};
    }
    
    private JLabel metCard(JPanel p, String label, Color bg, Color fg) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(bg);
        card.setBorder(new EmptyBorder(5, 8, 5, 8));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 8));
        lbl.setForeground(fg);

        JLabel val = new JLabel("—");
        val.setFont(new Font("Segoe UI", Font.BOLD, 14));
        val.setForeground(fg);

        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        p.add(card);
        p.add(Box.createVerticalStrut(4));
        return val;
    }

    private JButton styledBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(6, 10, 6, 10));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return b;
    }

    private JButton barBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 10));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setOpaque(true);
        return b;
    }

    class NetworkPanel extends JPanel {
        private List<Integer> path   = new ArrayList<>();
        private double[][]    pherom = null;
        private boolean       isIACO = true;

        NetworkPanel() {
            setBackground(new Color(0xFAFAF8));
        }

        void update(List<Integer> p, double[][] ph, boolean iaco) {
            this.path   = new ArrayList<>(p);
            this.pherom = ph;
            this.isIACO = iaco;
            repaint();
        }

        void reset() { path.clear(); pherom = null; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (px == null || N == 0) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            int cx = W / 2, cy = H / 2;
            int R = Math.min(W, H) / 2 - 50;

            // map logical coords [-1,1] to screen
            int[] sx = new int[N], sy = new int[N];
            for (int i = 0; i < N; i++) {
                sx[i] = cx + (int)(px[i] * R);
                sy[i] = cy + (int)(py[i] * R);
            }

            // draw all edges (faint)
            g2.setStroke(new BasicStroke(0.7f));
            for (int i = 0; i < N; i++) {
                for (int j = i + 1; j < N; j++) {
                    g2.setColor(C_EDGE);
                    g2.drawLine(sx[i], sy[i], sx[j], sy[j]);
                }
            }

            // draw best path
            if (!path.isEmpty()) {
                Color pathColor = isIACO ? C_IACO : C_ACO;
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND,
                                              BasicStroke.JOIN_ROUND));
                g2.setColor(pathColor);
                for (int k = 0; k < path.size() - 1; k++) {
                    int a = path.get(k), b = path.get(k + 1);
                    g2.drawLine(sx[a], sy[a], sx[b], sy[b]);

                    // arrowhead
                    drawArrow(g2, sx[a], sy[a], sx[b], sy[b], pathColor);
                }
            }

            // draw nodes
            int nr = 14;
            for (int i = 0; i < N; i++) {
                boolean isStart = (i == 0);
                g2.setColor(isStart ? C_START : new Color(0xB5D4F4));
                g2.fillOval(sx[i] - nr, sy[i] - nr, nr * 2, nr * 2);
                g2.setColor(C_IACO);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawOval(sx[i] - nr, sy[i] - nr, nr * 2, nr * 2);

                g2.setColor(isStart ? Color.WHITE : C_NODE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                FontMetrics fm = g2.getFontMetrics();
                String lbl = String.valueOf(i);
                g2.drawString(lbl,
                    sx[i] - fm.stringWidth(lbl) / 2,
                    sy[i] + fm.getAscent() / 2 - 1);
            }

            // legend
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            String alg  = path.isEmpty() ? "No run yet" : (isIACO ? "IACO" : "ACO");
            String costS = "";
            if (!path.isEmpty()) {
                double c = tourCost(path);
                costS = "  —  cost: " + String.format("%.2f", c);
            }
            g2.setColor(new Color(0x2C2C2A));
            g2.drawString("Network Graph  •  " + alg + costS, 12, 20);

            drawLegend(g2, W, H, isIACO, !path.isEmpty());
        }

        private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2, Color c) {
            double dx = x2 - x1, dy = y2 - y1;
            double len = Math.sqrt(dx*dx + dy*dy);
            if (len < 1) return;
            double ux = dx / len, uy = dy / len;
            // place arrow near midpoint
            double mx = (x1 + x2) / 2.0, my = (y1 + y2) / 2.0;
            int as = 9;
            int[] xp = {(int)(mx + ux*as), (int)(mx - ux*as - uy*as/2),
                         (int)(mx - ux*as + uy*as/2)};
            int[] yp = {(int)(my + uy*as), (int)(my - uy*as + ux*as/2),
                         (int)(my - uy*as - ux*as/2)};
            g2.setColor(c);
            g2.fillPolygon(xp, yp, 3);
        }

        private void drawLegend(Graphics2D g2, int W, int H, boolean iaco, boolean hasTour) {
            int lx = W - 165, ly = H - 90, lw = 155, lh = 78;
            g2.setColor(new Color(255, 255, 255, 210));
            g2.fillRoundRect(lx, ly, lw, lh, 8, 8);
            g2.setColor(C_BORDER);
            g2.drawRoundRect(lx, ly, lw, lh, 8, 8);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            int ry = ly + 16;
            legendRow(g2, lx + 8, ry, C_START, "Host 0 (start)"); ry += 18;
            legendRow(g2, lx + 8, ry, new Color(0xB5D4F4), "Host computer"); ry += 18;
            if (hasTour) {
                legendRow(g2, lx + 8, ry, iaco ? C_IACO : C_ACO,
                          iaco ? "IACO best path" : "ACO best path");
            }
        }

        private void legendRow(Graphics2D g2, int x, int y, Color c, String text) {
            g2.setColor(c);
            g2.fillRect(x, y - 8, 14, 10);
            g2.setColor(new Color(0x444441));
            g2.drawString(text, x + 18, y);
        }
    }

    // convergence line chart
    class ConvergPanel extends JPanel {
        private List<Double> iaco = new ArrayList<>();
        private List<Double> aco  = new ArrayList<>();

        ConvergPanel() { setBackground(new Color(0xFAFAF8)); }

        void setIACO(List<Double> h) { iaco = new ArrayList<>(h); repaint(); }
        void setACO (List<Double> h) { aco  = new ArrayList<>(h); repaint(); }
        void reset() { iaco.clear(); aco.clear(); repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            int px2 = 60, py2 = 30, pw = W - px2 - 30, ph = H - py2 - 50;

            // axes
            g2.setColor(C_BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(px2, py2, pw, ph);

            // grid lines
            g2.setColor(new Color(0xE8E8E4));
            g2.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_BUTT,
                                          BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
            for (int i = 1; i < 5; i++) {
                int gy = py2 + ph * i / 5;
                g2.drawLine(px2, gy, px2 + pw, gy);
            }

            if (iaco.isEmpty() && aco.isEmpty()) {
                g2.setColor(C_MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.drawString("Run algorithms to see convergence", px2 + pw/4, py2 + ph/2);
                return;
            }

            // compute scale
            double maxV = 0, minV = Double.MAX_VALUE;
            for (double v : iaco) { maxV = Math.max(maxV, v); minV = Math.min(minV, v); }
            for (double v : aco)  { maxV = Math.max(maxV, v); minV = Math.min(minV, v); }
            if (maxV == minV) { maxV += 1; minV -= 1; }

            // draw curves
            drawCurve(g2, aco,  px2, py2, pw, ph, maxV, minV, C_ACO,  true);
            drawCurve(g2, iaco, px2, py2, pw, ph, maxV, minV, C_IACO, false);

            // axis labels
            g2.setColor(C_MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.drawString("Iteration", px2 + pw / 2 - 20, H - 8);
            // y-axis label (rotated)
            Graphics2D g2r = (Graphics2D) g2.create();
            g2r.rotate(-Math.PI / 2, 14, py2 + ph / 2);
            g2r.drawString("Best cost (lower = better)", 14, py2 + ph / 2);
            g2r.dispose();

            // title
            g2.setColor(new Color(0x2C2C2A));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.drawString("Convergence Curve  —  ACO vs IACO", px2 + pw / 2 - 110, 18);

            // legend
            int lx = px2 + pw - 140, ly = py2 + 10;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            if (!aco.isEmpty()) {
                g2.setColor(C_ACO);
                drawDashedLine(g2, lx, ly, lx + 20, ly);
                g2.setColor(C_MUTED);
                g2.drawString("ACO (baseline)", lx + 24, ly + 4);
                ly += 16;
            }
            if (!iaco.isEmpty()) {
                g2.setColor(C_IACO);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(lx, ly, lx + 20, ly);
                g2.setColor(C_MUTED);
                g2.drawString("IACO (improved)", lx + 24, ly + 4);
            }
        }

        private void drawCurve(Graphics2D g2, List<Double> data, int ox, int oy,
                                int w, int h, double maxV, double minV,
                                Color c, boolean dashed) {
            if (data.size() < 2) return;
            if (dashed) {
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 0, new float[]{8, 4}, 0));
            } else {
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND,
                                              BasicStroke.JOIN_ROUND));
            }
            g2.setColor(c);
            int n = data.size();
            int x0 = ox + 0, y0 = oy + h - (int)((data.get(0) - minV) / (maxV - minV) * h);
            for (int i = 1; i < n; i++) {
                int xi = ox + i * w / n;
                int yi = oy + h - (int)((data.get(i) - minV) / (maxV - minV) * h);
                g2.drawLine(x0, y0, xi, yi);
                x0 = xi; y0 = yi;
            }
        }

        private void drawDashedLine(Graphics2D g2, int x1, int y1, int x2, int y2) {
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                                          10, new float[]{5, 3}, 0));
            g2.drawLine(x1, y1, x2, y2);
            g2.setStroke(old);
        }
    }

    // ── pheromone heatmap ─────────────────────────────────────
    class PheromonePanel extends JPanel {
        private double[][] tau = null;

        PheromonePanel() { setBackground(new Color(0xFAFAF8)); }
        void update(double[][] t) { tau = deepCopy(t); repaint(); }
        void reset() { tau = null; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            if (tau == null) {
                g2.setColor(C_MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.drawString("Run algorithm to see pheromone matrix",
                              getWidth()/2 - 150, getHeight()/2);
                return;
            }

            int W = getWidth(), H = getHeight();
            int pad = 50;
            int cells = tau.length;
            int cell = Math.min((W - pad * 2) / cells, (H - pad * 2) / cells);
            int ox = (W - cell * cells) / 2;
            int oy = (H - cell * cells) / 2;

            // find min/max for colour scaling
            double maxT = 0, minT = Double.MAX_VALUE;
            for (double[] row : tau) for (double v : row) {
                maxT = Math.max(maxT, v); minT = Math.min(minT, v);
            }
            if (maxT == minT) maxT = minT + 0.001;

            for (int i = 0; i < cells; i++) {
                for (int j = 0; j < cells; j++) {
                    float t = (float)((tau[i][j] - minT) / (maxT - minT));
                    // white → deep blue colour ramp
                    int r = (int)(240 - t * 190);
                    int gc = (int)(248 - t * 150);
                    int b = 255;
                    g2.setColor(new Color(clamp(r), clamp(gc), clamp(b)));
                    g2.fillRect(ox + j * cell, oy + i * cell, cell, cell);
                }
            }

            // grid
            g2.setColor(new Color(180, 180, 180, 80));
            g2.setStroke(new BasicStroke(0.5f));
            for (int i = 0; i <= cells; i++) {
                g2.drawLine(ox + i * cell, oy, ox + i * cell, oy + cells * cell);
                g2.drawLine(ox, oy + i * cell, ox + cells * cell, oy + i * cell);
            }

            // axis labels (every 5)
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
            g2.setColor(C_MUTED);
            for (int i = 0; i < cells; i += Math.max(1, cells / 8)) {
                g2.drawString(String.valueOf(i), ox + i * cell + cell/2 - 3, oy - 4);
                g2.drawString(String.valueOf(i), ox - 18, oy + i * cell + cell/2 + 3);
            }

            // colour bar
            int bx = ox + cells * cell + 10, by = oy, bh = cells * cell, bw = 14;
            for (int i = 0; i < bh; i++) {
                float t = 1f - (float) i / bh;
                int r = (int)(240 - t * 190), gc = (int)(248 - t * 150);
                g2.setColor(new Color(clamp(r), clamp(gc), 255));
                g2.fillRect(bx, by + i, bw, 1);
            }
            g2.setColor(C_BORDER);
            g2.drawRect(bx, by, bw, bh);
            g2.setColor(C_MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
            g2.drawString(String.format("%.2f", maxT), bx + bw + 3, by + 6);
            g2.drawString(String.format("%.2f", minT), bx + bw + 3, by + bh);

            // title
            g2.setColor(new Color(0x2C2C2A));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.drawString("Pheromone Matrix  (post-run state)", ox, oy - 16);
        }

        private int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(IACO_Final_GUI::new);
    }
}
