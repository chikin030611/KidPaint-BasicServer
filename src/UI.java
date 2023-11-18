import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.util.Stack;
import javax.swing.border.LineBorder;

enum PaintMode {Pixel, Area};

public class UI extends JFrame {

	/** Variable Declarition */
	private JTextField messageField;
	private JTextArea chatArea;
	private JPanel pnlColorPicker;
	private JPanel paintPanel;
	private JToggleButton penToggleButton;
	private JToggleButton bucketToggleButton;
	private JToggleButton eraserToggleButton;
	private  JToggleButton loadToggleButton;
	private JToggleButton saveToggleButton;
	private JButton undoButton;
	private JButton redoButton;
	private JFileChooser fileChooser = new JFileChooser();

	private boolean eraserMode = false;
	String name;
	DataOutputStream out;
	private static UI instance;
	private int selectedColor = -543230;
	int[][] panel = new int[50][50];
	int blockSize = 16;
	private Stack<int[][]> undoStack = new Stack<>();
	private Stack<int[][]> redoStack = new Stack<>();
	PaintMode paintMode = PaintMode.Pixel;
	/** End of Variable Declaration */

	public static UI getInstance() {
		return instance;
	}

	/**
	 * get the instance of UI. Singleton design pattern.
	 */
	public static UI getInstance(String serverIP, int port, String name) throws IOException {
		if (instance == null)
			instance = new UI(serverIP, port, name);
		return instance;
	}

	/**
	 * private constructor. To create an instance of UI, call UI.getInstance() instead.
	 */
	private UI(String serverIP, int port, String name) throws IOException {
		setTitle("KidPaint");

		this.name = name;
		Socket socket = new Socket(serverIP, port);
		out = new DataOutputStream(socket.getOutputStream());
		Thread t = new Thread(() -> {
			receiveData(socket);
		});
		t.start();

		JPanel basePanel = new JPanel();
		getContentPane().add(basePanel, BorderLayout.CENTER);
		basePanel.setLayout(new BorderLayout(0, 0));

		paintPanel = new JPanel() {

			// refresh the paint panel
			@Override
			public void paint(Graphics g) {
				super.paint(g);

				Graphics2D g2 = (Graphics2D) g; // Graphics2D provides the setRenderingHints method

				// enable anti-aliasing
			    RenderingHints rh = new RenderingHints(
			             RenderingHints.KEY_ANTIALIASING,
			             RenderingHints.VALUE_ANTIALIAS_ON);
			    g2.setRenderingHints(rh);

			    // clear the paint panel using black
				g2.setColor(Color.black);
				g2.fillRect(0, 0, this.getWidth(), this.getHeight());

				// draw and fill circles with the specific colors stored in the data array
				for(int x = 0; x< panel.length; x++) {
					for (int y = 0; y< panel[0].length; y++) {
						g2.setColor(new Color(panel[x][y]));
						g2.fillArc(blockSize * x, blockSize * y, blockSize, blockSize, 0, 360);
						g2.setColor(Color.darkGray);
						g2.drawArc(blockSize * x, blockSize * y, blockSize, blockSize, 0, 360);
					}
				}
			}
		};

		paintPanel.addMouseListener(new MouseListener() {
			@Override public void mouseClicked(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e) {}

			// handle the mouse-up event of the paint panel
			@Override
			public void mouseReleased(MouseEvent e) {
				if (paintMode == PaintMode.Area && e.getX() >= 0 && e.getY() >= 0) {
					try {
						paintArea(e.getX()/blockSize, e.getY()/blockSize);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		});

		paintPanel.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				try {
					if (paintMode == PaintMode.Pixel && e.getX() >= 0 && e.getY() >= 0) {
						try {
							paintPixel(e.getX() / blockSize, e.getY() / blockSize);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				} catch (ArrayIndexOutOfBoundsException ex) {}
			}
			@Override public void mouseMoved(MouseEvent e) {}
		});

		paintPanel.setPreferredSize(new Dimension(panel.length * blockSize, panel[0].length * blockSize));

		JScrollPane scrollPaneLeft = new JScrollPane(paintPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		basePanel.add(scrollPaneLeft, BorderLayout.CENTER);

		JPanel toolPnl = new JPanel();
		basePanel.add(toolPnl, BorderLayout.NORTH);
		toolPnl.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		pnlColorPicker = new JPanel();
		pnlColorPicker.setPreferredSize(new Dimension(24, 24));
		pnlColorPicker.setBackground(new Color(selectedColor));
		pnlColorPicker.setBorder(new LineBorder(new Color(0, 0, 0)));

		// show the color picker
		pnlColorPicker.addMouseListener(new MouseListener() {
			@Override public void mouseClicked(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {
				ColorPicker picker = ColorPicker.getInstance(UI.instance);
				Point location = pnlColorPicker.getLocationOnScreen();
				location.y += pnlColorPicker.getHeight();
				picker.setLocation(location);
				picker.setVisible(true);
			}

		});

		toolPnl.add(pnlColorPicker);

		penToggleButton = new JToggleButton("Pen");
		penToggleButton.setSelected(true);
		toolPnl.add(penToggleButton);

		bucketToggleButton = new JToggleButton("Bucket");
		toolPnl.add(bucketToggleButton);

		eraserToggleButton = new JToggleButton("Eraser");
		toolPnl.add(eraserToggleButton);
		eraserToggleButton.setSelected(false);

		undoButton = new JButton("Undo");
		toolPnl.add(undoButton);

		redoButton = new JButton("Redo");
		toolPnl.add(redoButton);

		loadToggleButton = new JToggleButton("Import");
		toolPnl.add(loadToggleButton);
		loadToggleButton.setSelected(false);

		saveToggleButton = new JToggleButton("Save");
		toolPnl.add(saveToggleButton);
		saveToggleButton.setSelected(false);

		penToggleButton.addActionListener(e -> {
			penToggleButton.setSelected(true);
			bucketToggleButton.setSelected(false);
			eraserToggleButton.setSelected(false);
			eraserMode = false;
			paintMode = PaintMode.Pixel;
		});

		bucketToggleButton.addActionListener(e-> {
			penToggleButton.setSelected(false);
			bucketToggleButton.setSelected(true);
			eraserToggleButton.setSelected(false);
			eraserMode = false;
			paintMode = PaintMode.Area;
		});

		eraserToggleButton.addActionListener(e ->{
			eraserToggleButton.setSelected(true);
			penToggleButton.setSelected(false);
			bucketToggleButton.setSelected(false);
			eraserMode = true;
			paintMode = PaintMode.Pixel;
		});

		undoButton.addActionListener(e -> {
			if (!undoStack.isEmpty()) {
				redoStack.push(cloneArray(panel)); // Save current state before undoing
				panel = undoStack.pop(); // Set panel to previous state
				paintPanel.repaint(); // Redraw the panel
				// send entire panel state
				for(int x = 0; x< panel.length; x++) {
					for (int y = 0; y< panel[0].length; y++) {
						try {
							out.writeInt(0);
							String p = x + " " + y + " " + panel[x][y];
							out.writeInt(p.length());
							out.write(p.getBytes(), 0, p.length());
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		});

		redoButton.addActionListener(e -> {
			if (!redoStack.isEmpty()) {
				undoStack.push(cloneArray(panel)); // Save current state before redoing
				panel = redoStack.pop(); // Set panel to next state
				paintPanel.repaint(); // Redraw the panel
				// send entire panel state
				for(int x = 0; x< panel.length; x++) {
					for (int y = 0; y< panel[0].length; y++) {
						try {
							out.writeInt(0);
							String p = x + " " + y + " " + panel[x][y];
							out.writeInt(p.length());
							out.write(p.getBytes(), 0, p.length());
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		});


		// Load data
		loadToggleButton.addActionListener(e -> {
			int[][] temp;
			int r = fileChooser.showOpenDialog(null);
			if (r == JFileChooser.APPROVE_OPTION) {
				try {
					temp = SaveAndLoad.load(fileChooser.getSelectedFile().getAbsolutePath());
					for (int i = 0; i < SaveAndLoad.row; i++) {
						for (int j = 0; j < SaveAndLoad.col; j++) {
							out.writeInt(0);
							String p = i + " " + j + " " + temp[i][j];
							out.writeInt(p.length());
							out.write(p.getBytes(), 0, p.length());
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			loadToggleButton.setSelected(false);
		});

		// Save file
		saveToggleButton.addActionListener(e -> {
			int r = fileChooser.showSaveDialog(null);
			if (r == JFileChooser.APPROVE_OPTION) {
				try {
					SaveAndLoad.save(fileChooser.getSelectedFile().getAbsolutePath(), panel);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			saveToggleButton.setSelected(false);
		});

		JPanel messagePanel = new JPanel();

		getContentPane().add(messagePanel, BorderLayout.EAST);

		messagePanel.setLayout(new BorderLayout(0, 0));

		messageField = new JTextField();	// text field for inputting message

		messagePanel.add(messageField, BorderLayout.SOUTH);

		// handle key-input event of the message field
		messageField.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == 10) {		// if the user press ENTER
					onTextInputted(messageField.getText());
					messageField.setText("");
				}
			}
		});

		chatArea = new JTextArea();		// the read only text area for showing messages
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);

		JScrollPane scrollPaneRight = new JScrollPane(chatArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPaneRight.setPreferredSize(new Dimension(300, this.getHeight()));
		messagePanel.add(scrollPaneRight, BorderLayout.CENTER);

		this.setSize(new Dimension(1335, 1095));
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	} // End of constructor

	/**
	 * it will be invoked if the user selected the specific color through the color picker
	 * @param colorValue - the selected color
	 */
	public void selectColor(int colorValue) {
		SwingUtilities.invokeLater(()->{
			selectedColor = colorValue;
			pnlColorPicker.setBackground(new Color(colorValue));
		});
	}

	/**
	 * it will be invoked if the user inputted text in the message field
	 * @param text - user inputted text
	 */
	private void onTextInputted(String text) {
		try {
			text = name + ": " + text;
			out.writeInt(-1);
			out.writeInt(text.length());
			out.write(text.getBytes(), 0, text.length());
		} catch (IOException e) {
			chatArea.append("Unable to send message to the server!\n");
		}
	}

	/**
	 * Saves the current state of the panel into the undo stack.
	 * The current state is represented by a 2D array of integers.
	 * Each element in the array represents the color of a pixel in the panel.
	 * The method copies the contents of the panel into a new array and pushes it onto the undo stack.
	 * After saving the current state, the redo stack is cleared.
	 */
	private void saveCurrentState() {
		int[][] currentState = new int[panel.length][panel[0].length];
		for (int i = 0; i < panel.length; i++) {
			System.arraycopy(panel[i], 0, currentState[i], 0, panel[i].length);
		}
		undoStack.push(currentState);
		redoStack.clear();
	}

	/**
	 * change the color of a specific pixel
	 * @param col, row - the position of the selected pixel
	 */
	public void paintPixel(int col, int row) throws IOException, ArrayIndexOutOfBoundsException {
		if (!eraserMode && panel[col][row] != selectedColor) saveCurrentState();
		else if (eraserMode && panel[col][row] == selectedColor) saveCurrentState();

		if (col >= panel.length || row >= panel[0].length) return;

		panel[col][row] = selectedColor;
		paintPanel.repaint(col * blockSize, row * blockSize, blockSize, blockSize);

		panel[col][row] = eraserMode ? 0 : selectedColor;

		out.writeInt(0);
		String p = col + " " + row + " " + panel[col][row];

		out.writeInt(p.length());
		out.write(p.getBytes(), 0, p.length());

	}

	/**
	 * change the color of a specific area
	 * @param col, row - the position of the selected pixel
	 * @return a list of modified pixels
	 */
	public List paintArea(int col, int row) throws IOException {
		if (panel[col][row] != selectedColor) saveCurrentState();

		LinkedList<Point> filledPixels = new LinkedList<Point>();

		if (col >= panel.length || row >= panel[0].length) return filledPixels;

		int originalColor = panel[col][row];
		LinkedList<Point> buffer = new LinkedList<Point>();

		int tempColor = selectedColor;

		if (eraserMode) tempColor = 0;

		if (originalColor != tempColor) {
			buffer.add(new Point(col, row));

			while(!buffer.isEmpty()) {
				Point point = buffer.removeFirst();
				int x = point.x;
				int y = point.y;

				if (panel[x][y] != originalColor) continue;

				panel[x][y] = tempColor;

				out.writeInt(0);
				String p = x + " " + y + " " + panel[x][y];
				out.writeInt(p.length());
				out.write(p.getBytes(), 0, p.length());

				filledPixels.add(point);

				if (x > 0 && panel[x-1][y] == originalColor) buffer.add(new Point(x-1, y));
				if (x < panel.length - 1 && panel[x+1][y] == originalColor) buffer.add(new Point(x+1, y));
				if (y > 0 && panel[x][y-1] == originalColor) buffer.add(new Point(x, y-1));
				if (y < panel[0].length - 1 && panel[x][y+1] == originalColor) buffer.add(new Point(x, y+1));
			}
			paintPanel.repaint();
		}
		return filledPixels;
	}

	/**
	 * Creates a deep copy of a 2D integer array.
	 * 
	 * @param source the original array to be cloned
	 * @return a new array that is a deep copy of the source array
	 */
	private int[][] cloneArray(int[][] source) {
		int[][] clone = new int[source.length][];
		for (int i = 0; i < source.length; i++) {
			clone[i] = source[i].clone();
		}
		return clone;
	}

	/**
	 * set pixel data and block size
	 * @param data
	 * @param blockSize
	 */
	public void setData(int[][] data, int blockSize) {
		this.panel = data;
		this.blockSize = blockSize;
		paintPanel.setPreferredSize(new Dimension(data.length * blockSize, data[0].length * blockSize));
		paintPanel.repaint();
	}

	/**
	 * Receives data from the specified socket and updates the UI accordingly.
	 * 
	 * @param socket the socket to receive data from
	 */
	private void receiveData(Socket socket) {
		try {
			byte[] buffer = new byte[1024];
			DataInputStream in = new DataInputStream(socket.getInputStream());
			while (true) {
				int type = in.readInt();
				int len = in.readInt();
				in.read(buffer, 0, len);
				String content = new String(buffer, 0, len);

				if (type == 0) {
					String[] p = content.split(" ");
					int col = Integer.parseInt(p[0]);
					int row = Integer.parseInt(p[1]);
					int color = Integer.parseInt(p[2]);
					SwingUtilities.invokeLater(() -> {
						panel[col][row] = color;
						paintPanel.repaint(col * blockSize, row * blockSize, blockSize, blockSize);
					});
				}
				if (type == -1) {
					SwingUtilities.invokeLater(() -> {
						chatArea.append(content + "\n");

					});
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
