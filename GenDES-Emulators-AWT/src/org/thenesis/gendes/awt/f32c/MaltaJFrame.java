package org.thenesis.gendes.awt.f32c;

import java.awt.BorderLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.thenesis.gendes.mips32.Cpu;
import org.thenesis.gendes.mips32.CpuConstants;
import org.thenesis.gendes.mips32.CpuExceptionParams;
import org.thenesis.gendes.mips32.CpuUtil;
import org.thenesis.gendes.mips32.util.Log;

public class MaltaJFrame extends JFrame implements PropertyChangeListener {
	
	public static void main (String[] args) {
		ToolTipManager.sharedInstance().setDismissDelay(60000);
		MaltaJFrame frame = new MaltaJFrame();
		frame.setVisible(true);
	}
	
	public static final Font MONO = new Font("Monospaced", Font.PLAIN, 12);
	
	private final JTextField fileField = new JTextField(20);
	private final JTextField argsField = new JTextField(20);
	private final JTextField displayTextField = new JTextField(8);
	private final JTextField displayLedField = new JTextField(8);
	private final JTextField displayWordField = new JTextField(8);
	private final JTextField cycleLabel = new JTextField(20);
	private final JTextArea consoleArea = new JTextArea();
	private final JButton fileButton = new JButton("File");
	private final JButton loadButton = new JButton("Load");
	private final JButton runButton = new JButton("Run");
	private final JButton stopButton = new JButton("Stop");
	private final JSpinner memSpinner = new JSpinner(new SpinnerNumberModel(32,32,512,1));
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final SymbolJPanel symbolsPanel = new SymbolJPanel();
	private final LoggerJPanel loggerPanel = new LoggerJPanel();
	private final MemoryJPanel memoryPanel = new MemoryJPanel();
	private final Timer timer;
	
	private volatile Thread thread;
	private Cpu cpu;
	
	public MaltaJFrame () {
		super("Sysmips");
		
		timer = new Timer(1000, e -> updateCycle());
		
		loadButton.addActionListener(ae -> load());
		runButton.addActionListener(ae -> run());
		
		// should load this from prefs
//		fileField.setText("images/vmlinux-3.2.0-4-4kc-malta");
		fileButton.addActionListener(ae -> selectFile());
		
		// command line of console=ttyS0 initrd=? root=?
		// environment keys: ethaddr, modetty0, memsize (defaults to 32MB)
		argsField.setText("debug initcall_debug ignore_loglevel");
		
		stopButton.addActionListener(ae -> stop());
		
		displayTextField.setFont(MONO);
		displayTextField.setEditable(false);
		displayLedField.setFont(MONO);
		displayLedField.setEditable(false);
		displayWordField.setFont(MONO);
		displayWordField.setEditable(false);
		
		cycleLabel.setFont(MONO);
		cycleLabel.setEditable(false);
		
		consoleArea.setColumns(100);
		consoleArea.setRows(24);
		consoleArea.setFont(MONO);
		consoleArea.setLineWrap(true);
		consoleArea.setEditable(false);
		
		JPanel topPanel1 = new JPanel();
		topPanel1.add(new JLabel("File"));
		topPanel1.add(fileField);
		topPanel1.add(fileButton);
		topPanel1.add(new JLabel("Mem"));
		topPanel1.add(memSpinner);
		topPanel1.add(new JLabel("Args"));
		topPanel1.add(argsField);
		topPanel1.add(loadButton);
		//topPanel1.add(new JLabel("Env"));
		//topPanel1.add(envField);
		
		JPanel topPanel2 = new JPanel();
		topPanel2.add(new JLabel("Display"));
		topPanel2.add(displayTextField);
		topPanel2.add(displayLedField);
		topPanel2.add(displayWordField);
		topPanel2.add(new JLabel("Cycle"));
		topPanel2.add(cycleLabel);
		topPanel2.add(runButton);
		topPanel2.add(stopButton);
		
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.add(topPanel1);
		topPanel.add(topPanel2);
		
		JScrollPane consoleSp = new JScrollPane(consoleArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		consoleSp.setBorder(new EmptyBorder(5,5,5,5));
		
		tabbedPane.add("Console", consoleSp);
		tabbedPane.add("Symbols", symbolsPanel);
		tabbedPane.add("Memory", memoryPanel);
		tabbedPane.add("Logger", loggerPanel);
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(topPanel, BorderLayout.NORTH);
		contentPanel.add(tabbedPane, BorderLayout.CENTER);
		contentPanel.setBorder(new EmptyBorder(5,5,5,5));
		
		loadPrefs();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setContentPane(contentPanel);
		pack();
	}
	
	private void loadPrefs () {
		Preferences prefs = Preferences.userNodeForPackage(getClass());
		fileField.setText(prefs.get("file", "images/vmlinux-3.2.0-4-4kc-malta"));
	}
	
	private void storePrefs () {
		Preferences prefs = Preferences.userNodeForPackage(getClass());
		prefs.put("file", fileField.getText());
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setVisible (boolean b) {
		if (b) {
			timer.start();
		} else {
			timer.stop();
		}
		super.setVisible(b);
	}

	private void stop () {
		if (cpu != null) {
			cpu.addException(new CpuExceptionParams(CpuConstants.EX_TRAP));
		}
	}

	private void selectFile () {
		File f = new File(fileField.getText());
		File dir = f.exists() ? f.getParentFile() : new File(System.getProperty("user.dir"));
		JFileChooser fc = new JFileChooser(dir);
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			fileField.setText(fc.getSelectedFile().toString());
		}
	}

	public void setFile(String file) {
		fileField.setText(file);
	}
	
	private void load () {
		if (thread != null) {
			showErrorDialog("Run", "Already running");
			return;
		}
		
		final Path path = Paths.get(fileField.getText());
		if (!path.toFile().isFile()) {
			showErrorDialog("Run", "Invalid file: " + path);
			return;
		}
		
		final int memsize = ((Integer)memSpinner.getValue()).intValue() * 0x100000;
		
		List<String> args = new ArrayList<>();
		// linux ignores first arg...
		args.add("linux");
		{
			StringTokenizer st = new StringTokenizer(argsField.getText());
			while (st.hasMoreTokens()) {
				args.add(st.nextToken());
			}
		}
		
		List<String> env = new ArrayList<>();
		env.add("memsize");
		env.add(String.valueOf(memsize));
		
		consoleArea.setText("");
		
		final Cpu cpu;
		
		try (FileChannel chan = FileChannel.open(path, StandardOpenOption.READ)) {
			int[] top = new int[1];
			cpu = CpuUtil.loadElf(chan, memsize, top);
			CpuUtil.setMainArgs(cpu, top[0] + 0x100000, args, env);
			
		} catch (Exception e) {
			e.printStackTrace();
			showErrorDialog("Start", e);
			return;
		}
		
		cpu.getMemory().print(System.out);
		cpu.getSupport().addPropertyChangeListener(this);
		
		this.cpu = cpu;
		
		symbolsPanel.setSymbols(cpu.getSymbols());
		memoryPanel.setCpu(cpu);
		
		updateCycle();
	}
	
	private void run() {
		System.out.println("run");
		
		if (thread != null) {
			return;
		}
		
		if (cpu == null) {
			load();
			if (cpu == null) {
				return;
			}
		}
		
		storePrefs();
		
		Thread t = new Thread(() -> {
			try {
				cpu.run();
				
			} catch (Exception e) {
				System.out.println();
				e.printStackTrace(System.out);
				
				SwingUtilities.invokeLater(() -> {
					consoleArea.append(exceptionString(e));
					updateCycle();
					showErrorDialog("Start", e);
				});
				
			} finally {
				this.thread = null;
			}
		});
		t.setName("Cpu-" + t.getName());
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
		
		thread = t;
	}

	private void showErrorDialog(String title, Throwable t) {
		showErrorDialog(title, exceptionString(t));
	}

	private static String exceptionString (Throwable t) {
		try (StringWriter sw = new StringWriter()) {
			try (PrintWriter pw = new PrintWriter(sw)) {
				t.printStackTrace(pw);
				return sw.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private void showErrorDialog(String title, String msg) {
		JOptionPane.showMessageDialog(this, wrap(msg, 100), title, JOptionPane.ERROR_MESSAGE);
	}

	public static String wrap (String msg, int max) {
		StringBuilder sb = new StringBuilder();
		int l = 0;
		for (int n = 0; n < msg.length(); n++) {
			final char c = msg.charAt(n);
			sb.append(c);
			l++;
			if (c == '\n') {
				l = 0;
			}
			if ((l > ((max*3)/4) && !Character.isLetterOrDigit(c)) || l >= max) {
				sb.append("\n");
				l = 0;
			}
		}
		return sb.toString();
	}

	@Override
	public void propertyChange (PropertyChangeEvent evt) {
		// System.out.println("jframe event " + evt);
		SwingUtilities.invokeLater(() -> update(evt.getPropertyName(), evt.getNewValue()));
	}
	
	private void update (String propertyName, Object value) {
		// System.out.println("jframe update " + propertyName);
		switch (propertyName) {
			case "displaytext":
				displayTextField.setText((String) value);
				displayTextField.repaint();
				break;
			case "displayled": {
				char[] a = new char[8];
				int v = ((Integer) value).intValue();
				for (int n = 0; n < 8; n++) {
					a[n] = (v & (1 << (7 - n))) != 0 ? '#' : ' ';
				}
				displayLedField.setText(new String(a));
				displayTextField.repaint();
				break;
			}
			case "displayword": {
				int v = ((Integer) value).intValue();
				displayWordField.setText(Integer.toHexString(v));
				displayWordField.repaint();
				break;
			}
			case "console": {
				Document doc = consoleArea.getDocument();
				try {
					doc.insertString(doc.getLength(), (String) value, null);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
				consoleArea.setCaretPosition(doc.getLength());
				break;
			}
			case "logs":
				loggerPanel.addLogs((Log[])value);
				break;
			default:
				throw new RuntimeException("unknown property " + propertyName);
		}
	}
	
	private void updateCycle () {
		if (cpu != null) {
			cycleLabel.setText(NumberFormat.getInstance().format(cpu.getCycle()));
		}
	}

	public void openMemory (int addr) {
		memoryPanel.openMemory(addr);
		tabbedPane.setSelectedComponent(memoryPanel);
	}
	
}
