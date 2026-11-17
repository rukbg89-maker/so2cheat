setTimeout(() => {
    try {
        const Socket = Java.use("java.net.Socket");
        const s = Socket.$new("127.0.0.1", 13338);
        const pw = Java.use("java.io.PrintWriter").$new(s.getOutputStream(), true);
        pw.println("[]");
    } catch (e) {}
}, 1000);
