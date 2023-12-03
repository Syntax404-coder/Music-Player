import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JavaGUIMusicPlayerJFrame extends JFrame implements ActionListener {

    private JTextField filePathField;
    private JButton chooseButton;
    private JButton playButton;
    private JButton pauseButton;
    private JButton loopButton;
    private boolean isPaused;
    private boolean isLooping = false;
    private JFileChooser fileChooser;
    private Clip clip;
    private MusicVisualizationPanel visualizationPanel;
    private JProgressBar progressBar;
    private JLabel timeLabel;

    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 50;

    public JavaGUIMusicPlayerJFrame() {
        super("Music Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        filePathField = new JTextField(20);
        chooseButton = createButton("C:\\Users\\Client\\Desktop\\menu-line.png");
        playButton = createButton("C:\\Users\\Client\\Desktop\\play-line.png");
        pauseButton = createButton("C:\\Users\\Client\\Desktop\\pause-line.png");
        loopButton = createButton("C:\\Users\\Client\\Desktop\\loop-left-line.png");
        isPaused = false;
        isLooping = false;

        chooseButton.addActionListener(this);
        playButton.addActionListener(this);
        pauseButton.addActionListener(this);
        loopButton.addActionListener(this);

        visualizationPanel = new MusicVisualizationPanel();
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        timeLabel = new JLabel("0:00 / 0:00", SwingConstants.LEFT);
        timeLabel.setForeground(Color.RED);

        fileChooser = new JFileChooser(".");
        fileChooser.setFileFilter(new FileNameExtensionFilter("WAV Files", "wav"));

        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pathPanel.add(filePathField);
        topPanel.add(pathPanel, BorderLayout.CENTER);
        topPanel.add(chooseButton, BorderLayout.EAST);

        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(playButton);
        controlPanel.add(pauseButton);
        controlPanel.add(loopButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        bottomPanel.add(controlPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(visualizationPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        add(timeLabel, BorderLayout.WEST);

        setSize(750, 450);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JButton createButton(String imagePath) {
        JButton button = new JButton(new ImageIcon(imagePath));
        button.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        button.setOpaque(false); // Set background to be transparent
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false); // Ensure the content area is not filled
        return button;
    }

    private void updateProgressBar() {
        if (clip != null) {
            int currentFrame = clip.getFramePosition();
            int totalFrames = clip.getFrameLength();

            long remainingSeconds = currentFrame / (long) clip.getFormat().getFrameRate();
            long totalDurationSeconds = totalFrames / (long) clip.getFormat().getFrameRate();

            String remainingTime = String.format("%d:%02d", remainingSeconds / 60, remainingSeconds % 60);
            String totalDurationTime = String.format("%d:%02d", totalDurationSeconds / 60, totalDurationSeconds % 60);

            timeLabel.setText(remainingTime + " / " + totalDurationTime);

            progressBar.setForeground(Color.RED);

            int progress = (int) ((double) currentFrame / totalFrames * 100);
            progressBar.setValue(progress);
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == playButton) {
            playMusic();
        } else if (event.getSource() == pauseButton) {
            pauseMusic();
        } else if (event.getSource() == chooseButton) {
            chooseFile();
        } else if (event.getSource() == loopButton) {
            toggleLoop();
        }
    }

    private void playMusic() {
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            visualizationPanel.stopVisualization();
        }

        try {
            File file = new File(filePathField.getText());
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(file);

            clip = AudioSystem.getClip();

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    visualizationPanel.stopVisualization();
                }
            });

            clip.open(audioIn);

            if (isLooping) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }

            visualizationPanel.setAudioData(file);
            visualizationPanel.startVisualization();

            clip.start();

            Timer progressTimer = new Timer(500, e -> updateProgressBar());
            progressTimer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pauseMusic() {
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
                visualizationPanel.stopVisualization();
                isPaused = true;
                // Update the pauseButton icon accordingly
            } else if (isPaused) {
                clip.start();

                if (isLooping) {
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                }

                visualizationPanel.startVisualization();
                isPaused = false;
                // Update the pauseButton icon accordingly
            }
        }
        updateProgressBar();
    }

    private void chooseFile() {
        fileChooser.setCurrentDirectory(new File("."));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void toggleLoop() {
        isLooping = !isLooping;
        if (isLooping) {
            // Update the loopButton icon accordingly

            if (clip != null && clip.isRunning()) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } else {
            // Update the loopButton icon accordingly

            if (clip != null && clip.isRunning()) {
                clip.loop(0);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JavaGUIMusicPlayerJFrame());
    }
}

class MusicVisualizationPanel extends JPanel {

    private byte[] audioData;
    private List<Integer> amplitudes;
    private Timer timer;
    private int bounceDirection = 1;

    public void setAudioData(File audioFile) {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(audioFile);
            audioData = new byte[(int) audioIn.getFrameLength() * audioIn.getFormat().getFrameSize()];
            audioIn.read(audioData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startVisualization() {
        amplitudes = new ArrayList<>();
        timer = new Timer(30, new ActionListener() {
            int frameStep = audioData.length / getWidth();
            int currentFrame = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (audioData != null) {
                    amplitudes.clear();

                    int maxAmplitude = 128;

                    for (int i = 0; i < getWidth(); i += 5) {
                        int value = Math.abs(audioData[currentFrame] * getHeight() / (2 * maxAmplitude));
                        amplitudes.add(value);
                        currentFrame = (currentFrame + frameStep * bounceDirection) % audioData.length;
                    }

                    if (currentFrame < 0) {
                        currentFrame = 0;
                        bounceDirection = 1;
                    } else if (currentFrame >= audioData.length) {
                        currentFrame = audioData.length - 1;
                        bounceDirection = -1;
                    }

                    repaint();
                }
            }
        });
        timer.start();
    }

    public void stopVisualization() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (amplitudes != null) {
            for (int i = 0; i < amplitudes.size(); i++) {
                int x1 = i * 5;
                int y1 = getHeight();
                int x2 = x1;
                int y2 = getHeight() - amplitudes.get(i);

                g.setColor(Color.RED);
                g.drawLine(x1, y1, x2, y2);
            }
        }
    }
}
