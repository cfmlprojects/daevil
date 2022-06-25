package daevil.term;

import com.google.common.base.Ascii;
import com.pty4j.PtyProcess;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Gobbler implements Runnable {
    private static final int WAIT_TIMEOUT_SECONDS = 10;
    private final Reader myReader;
    private final CountDownLatch myLatch;
    private final PtyProcess myProcess;
    private final StringBuffer myOutput;
    private final Thread myThread;
    private final BlockingQueue<String> myLineQueue = new LinkedBlockingQueue<>();
    private final ReentrantLock myNewTextLock = new ReentrantLock();
    private final Condition myNewTextCondition = myNewTextLock.newCondition();

    Gobbler(Reader reader, CountDownLatch latch, PtyProcess process) {
        myReader = reader;
        myLatch = latch;
        myProcess = process;
        myOutput = new StringBuffer();
        myThread = new Thread(this, "Stream gobbler");
        myThread.start();
    }

    public static String cleanWinText(String text) {
        if (com.sun.jna.Platform.isWindows()) {
            return cleanWinTextAnsi(text);
        }
        return text;
    }

    public static String cleanWinTextAnsi(String text) {
        text = text.replace("\u001B[0m", "").replace("\u001B[m", "").replace("\u001B[0K", "").replace("\u001B[K", "")
                .replace("\u001B[?25l", "").replace("\u001b[?25h", "").replaceAll("\u001b\\[\\d*G", "")
                .replace("\u001b[2J", "").replaceAll("\u001B\\[\\d*;?\\d*H", "")
                .replaceAll("\u001B\\[\\d*X", "")
                .replaceAll(" *\\r\\n", "\r\n").replaceAll(" *$", "").replaceAll("(\\r\\n)+\\r\\n$", "\r\n");
        int oscInd = 0;
        do {
            oscInd = text.indexOf("\u001b]0;", oscInd);
            int bellInd = oscInd >= 0 ? text.indexOf(Ascii.BEL, oscInd) : -1;
            if (bellInd >= 0) {
                text = text.substring(0, oscInd) + text.substring(bellInd + 1);
            }
        } while (oscInd >= 0);
        int backspaceInd = text.indexOf(Ascii.BS);
        while (backspaceInd >= 0) {
            text = text.substring(0, Math.max(0, backspaceInd - 1)) + text.substring(backspaceInd + 1);
            backspaceInd = text.indexOf(Ascii.BS);
        }
        return text;
    }

    @Override
    public void run() {
        try {
            char[] buf = new char[32 * 1024];
            String linePrefix = "";
            while (true) {
                int count = myReader.read(buf);
                if (count <= 0) {
                    myReader.close();
                    return;
                }
                myOutput.append(buf, 0, count);
                linePrefix = processLines(linePrefix + new String(buf, 0, count));
                myNewTextLock.lock();
                try {
                    myNewTextCondition.signalAll();
                } finally {
                    myNewTextLock.unlock();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (myLatch != null) {
                myLatch.countDown();
            }
        }
    }

    private String processLines(String text) {
        int start = 0;
        while (true) {
            int end = text.indexOf('\n', start);
            if (end < 0) {
                return text.substring(start);
            }
            myLineQueue.add(text.substring(start, end + 1));
            start = end + 1;
        }
    }

    public String getOutput() {
        return myOutput.toString();
    }

    public String getPlainOutput() {
        return cleanWinTextAnsi(myOutput.toString()).replaceAll("[\r|\n]+", "\n").trim();
    }

    public void awaitFinish() throws InterruptedException {
        myThread.join(TimeUnit.SECONDS.toMillis(WAIT_TIMEOUT_SECONDS));
    }

    public String readLine() throws InterruptedException {
        return readLine(TimeUnit.SECONDS.toMillis(WAIT_TIMEOUT_SECONDS));
    }

    public String readLine(long awaitTimeoutMillis) throws InterruptedException {
        String line = myLineQueue.poll(awaitTimeoutMillis, TimeUnit.MILLISECONDS);
        if (line != null) {
            line = cleanWinText(line);
        }
        return line;
    }

    protected boolean awaitTextEndsWith(String suffix, long timeoutMillis) {
        long startTimeMillis = System.currentTimeMillis();
        long nextTimeoutMillis = timeoutMillis;
        do {
            myNewTextLock.lock();
            try {
                try {
                    if (endsWith(suffix)) {
                        return true;
                    }
                    myNewTextCondition.await(nextTimeoutMillis, TimeUnit.MILLISECONDS);
                    if (endsWith(suffix)) {
                        return true;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
            } finally {
                myNewTextLock.unlock();
            }
            nextTimeoutMillis = startTimeMillis + timeoutMillis - System.currentTimeMillis();
        } while (nextTimeoutMillis >= 0);
        return false;
    }

    private boolean awaitTextContains(String string, long timeoutMillis) {
        long startTimeMillis = System.currentTimeMillis();
        long nextTimeoutMillis = timeoutMillis;
        do {
            myNewTextLock.lock();
            try {
                try {
                    if (contains(string)) {
                        return true;
                    }
                    myNewTextCondition.await(nextTimeoutMillis, TimeUnit.MILLISECONDS);
                    if (contains(string)) {
                        return true;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
            } finally {
                myNewTextLock.unlock();
            }
            nextTimeoutMillis = startTimeMillis + timeoutMillis - System.currentTimeMillis();
        } while (nextTimeoutMillis >= 0);
        return false;
    }

    private boolean endsWith(String suffix) {
        String text = cleanWinText(myOutput.toString());
        return text.trim().toLowerCase().endsWith(suffix.toLowerCase().trim());
    }

    private boolean contains(String searchTerm) {
        String text = cleanWinText(myOutput.toString());
        return text.toLowerCase().contains(searchTerm.toLowerCase());
    }

    public void assertEndsWith(String expectedSuffix) {
        assertEndsWith(expectedSuffix, TimeUnit.SECONDS.toMillis(WAIT_TIMEOUT_SECONDS));
    }

    private void assertEndsWith(String expectedSuffix, long timeoutMillis) {
        boolean ok = awaitTextEndsWith(expectedSuffix, timeoutMillis);
        if (!ok) {
            String output = getOutput();
            String cleanOutput = cleanWinText(output);
            String actual = cleanOutput.substring(Math.max(0, cleanOutput.length() - expectedSuffix.length()));
            if (expectedSuffix.equals(actual)) {
                throw new RuntimeException("awaitTextEndsWith could detect suffix within timeout, but it is there");
            }
            expectedSuffix = Console.convertInvisibleChars(expectedSuffix);
            actual = Console.convertInvisibleChars(actual);
            int lastTextSize = 1000;
            String lastText = output.substring(Math.max(0, output.length() - lastTextSize));
            if (output.length() > lastTextSize) {
                lastText = "..." + lastText;
            }
            if (expectedSuffix.equals(actual)) {
                throw new RuntimeException("Unmatched suffix (trailing text: " + Console.convertInvisibleChars(lastText) +
                        (myProcess != null ? ", " + Console.getProcessStatus(myProcess) : "") + ")");
            }
        }
    }
}
