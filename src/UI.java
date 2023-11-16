import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
	private JTextField msgField;
	private JTextArea chatArea;
	private JPanel pnlColorPicker;
	private JPanel paintPanel;
	private JToggleButton tglPen;
	private JToggleButton tglBucket;
	private JToggleButton tglEraser;
	private  JToggleButton tglImport;
	private JToggleButton tglSave;
	private JButton undoButton;
	private JButton redoButton;


	private JFileChooser fileChooser = new JFileChooser();
	private boolean eraseMode = false;
	String username;
	DataOutputStream out;

	private static UI instance;
	private int selectedColor = -543230; 	//golden

	int[][] data = new int[50][50];			// pixel color data array
	int blockSize = 16;
	private Stack<int[][]> undoStack = new Stack<>();
	private Stack<int[][]> redoStack = new Stack<>();
	PaintMode paintMode = PaintMode.Pixel;


	public static UI getInstance() {

		return instance;
	}

	/**
	 * get the instance of UI. Singleton design pattern.
	 * @return
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

		username = name;
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
				for(int x=0; x<data.length; x++) {
					for (int y=0; y<data[0].length; y++) {
						g2.setColor(new Color(data[x][y]));
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
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		paintPanel.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				if (paintMode == PaintMode.Pixel && e.getX() >= 0 && e.getY() >= 0) {
					try {
						paintPixel(e.getX()/blockSize,e.getY()/blockSize);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}


			}

			@Override public void mouseMoved(MouseEvent e) {}

		});

		paintPanel.setPreferredSize(new Dimension(data.length * blockSize, data[0].length * blockSize));

		JScrollPane scrollPaneLeft = new JScrollPane(paintPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		basePanel.add(scrollPaneLeft, BorderLayout.CENTER);

		JPanel toolPanel = new JPanel();
		basePanel.add(toolPanel, BorderLayout.NORTH);
		toolPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));


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

		toolPanel.add(pnlColorPicker);

		tglPen = new JToggleButton("Pen");
		tglPen.setSelected(true);
		toolPanel.add(tglPen);

		tglBucket = new JToggleButton("Bucket");
		toolPanel.add(tglBucket);

		tglEraser = new JToggleButton("Eraser");
		toolPanel.add(tglEraser);
		tglEraser.setSelected(false);

		undoButton = new JButton("Undo");
		undoButton.addActionListener(e -> undo());
		toolPanel.add(undoButton);

		redoButton = new JButton("Redo");
		redoButton.addActionListener(e -> redo());
		toolPanel.add(redoButton);


		tglImport = new JToggleButton("Import");
		toolPanel.add(tglImport);
		tglImport.setSelected(false);

		tglSave = new JToggleButton("Save");
		toolPanel.add(tglSave);
		tglSave.setSelected(false);


		// change the paint mode to PIXEL mode
		tglPen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(true);
				tglBucket.setSelected(false);
				tglEraser.setSelected(false);
				eraseMode = false;
				paintMode = PaintMode.Pixel;
			}
		});

		// change the paint mode to AREA mode
		tglBucket.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(false);
				tglBucket.setSelected(true);
				tglEraser.setSelected(false);
				eraseMode = false;
				paintMode = PaintMode.Area;
			}
		});

		// change to eraser mode
		tglEraser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglEraser.setSelected(true);
				tglPen.setSelected(false);
				tglBucket.setSelected(false);
				eraseMode = true;
				paintMode = PaintMode.Pixel;
			}
		});

		// import data
		tglImport.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int[][] temp;
				int r = fileChooser.showOpenDialog(null);
				if (r == JFileChooser.APPROVE_OPTION) {
					try {
						temp = LocalInputOutput.importFile(fileChooser.getSelectedFile().getAbsolutePath());
						for (int i = 0; i < LocalInputOutput.row; i++) {
							for (int j = 0; j < LocalInputOutput.col; j++) {
								out.writeInt(0);
								String p = i + " " + j + " " + temp[i][j];
								out.writeInt(p.length());
								out.write(p.getBytes(), 0, p.length());
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				tglImport.setSelected(false);
			}
		});

		// save file
		tglSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int r = fileChooser.showSaveDialog(null);
				if (r == JFileChooser.APPROVE_OPTION) {
					try {
						LocalInputOutput.save(fileChooser.getSelectedFile().getAbsolutePath(), data);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				tglSave.setSelected(false);
			}
		});

		JPanel msgPanel = new JPanel();

		getContentPane().add(msgPanel, BorderLayout.EAST);

		msgPanel.setLayout(new BorderLayout(0, 0));

		msgField = new JTextField();	// text field for inputting message

		msgPanel.add(msgField, BorderLayout.SOUTH);

		// handle key-input event of the message field
		msgField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == 10) {		// if the user press ENTER
					onTextInputted(msgField.getText());
					msgField.setText("");
					// send data to server

				}
			}

		});

		chatArea = new JTextArea();		// the read only text area for showing messages
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);

		JScrollPane scrollPaneRight = new JScrollPane(chatArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPaneRight.setPreferredSize(new Dimension(300, this.getHeight()));
		msgPanel.add(scrollPaneRight, BorderLayout.CENTER);

		this.setSize(new Dimension(800, 600));
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

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
//		chatArea.setText(chatArea.getText() + text + "\n");

		try {
			text = username + ": " + text;
			out.writeInt(-1);
			out.writeInt(text.length());
			out.write(text.getBytes(), 0, text.length());
		} catch (IOException e) {
			chatArea.append("Unable to send message to the server!\n");
		}
	}

	private void saveCurrentState() {
		int[][] currentState = new int[data.length][data[0].length];
		for (int i = 0; i < data.length; i++) {
			System.arraycopy(data[i], 0, currentState[i], 0, data[i].length);
		}
		undoStack.push(currentState);
		redoStack.clear();
	}

	/**
	 * change the color of a specific pixel
	 * @param col, row - the position of the selected pixel
	 */
	public void paintPixel(int col, int row) throws IOException {
		if (!eraseMode && data[col][row] != selectedColor) saveCurrentState();
		else if (eraseMode && data[col][row] == selectedColor) saveCurrentState();


		if (col >= data.length || row >= data[0].length) return;

		data[col][row] = selectedColor;
		paintPanel.repaint(col * blockSize, row * blockSize, blockSize, blockSize);

		data[col][row] = eraseMode ? 0 : selectedColor;

		out.writeInt(0);
		String p = col + " " + row + " " + data[col][row];

		out.writeInt(p.length());
		out.write(p.getBytes(), 0, p.length());

	}

	/**
	 * change the color of a specific area
	 * @param col, row - the position of the selected pixel
	 * @return a list of modified pixels
	 */
	public List paintArea(int col, int row) throws IOException {
		if (data[col][row] != selectedColor) saveCurrentState();

		LinkedList<Point> filledPixels = new LinkedList<Point>();

		if (col >= data.length || row >= data[0].length) return filledPixels;

		int oriColor = data[col][row];
		LinkedList<Point> buffer = new LinkedList<Point>();

		int tempColor = selectedColor;

		if (eraseMode) {
			tempColor = 0;
		}

		if (oriColor != tempColor) {
			buffer.add(new Point(col, row));

			while(!buffer.isEmpty()) {
				Point point = buffer.removeFirst();
				int x = point.x;
				int y = point.y;

				if (data[x][y] != oriColor) continue;

				data[x][y] = tempColor;

				out.writeInt(0);
				String p = x + " " + y + " " + data[x][y];
				out.writeInt(p.length());
				out.write(p.getBytes(), 0, p.length());

				filledPixels.add(point);

				if (x > 0 && data[x-1][y] == oriColor) buffer.add(new Point(x-1, y));
				if (x < data.length - 1 && data[x+1][y] == oriColor) buffer.add(new Point(x+1, y));
				if (y > 0 && data[x][y-1] == oriColor) buffer.add(new Point(x, y-1));
				if (y < data[0].length - 1 && data[x][y+1] == oriColor) buffer.add(new Point(x, y+1));
			}
			paintPanel.repaint();
		}
		return filledPixels;
	}

	/**
	 * undo the last operation
	 */
	public void undo() {
		if (!undoStack.isEmpty()) {
			redoStack.push(data);
			data = undoStack.pop();
			repaintPanel();
		}
	}

	/**
	 * redo the last undo operation
	 */
	public void redo() {
		if (!redoStack.isEmpty()) {
			undoStack.push(data);
			data = redoStack.pop();
			repaintPanel();
		}
	}

	private void repaintPanel() {
		paintPanel.repaint();
	}


	/**
	 * set pixel data and block size
	 * @param data
	 * @param blockSize
	 */
	public void setData(int[][] data, int blockSize) {
		this.data = data;
		this.blockSize = blockSize;
		paintPanel.setPreferredSize(new Dimension(data.length * blockSize, data[0].length * blockSize));
		paintPanel.repaint();
	}

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
						data[col][row] = color;
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
