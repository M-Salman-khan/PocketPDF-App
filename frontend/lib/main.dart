import 'dart:io';
import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import 'package:http/http.dart' as http;
import 'package:file_saver/file_saver.dart';

void main() {
  runApp(const MaterialApp(home: PDFCompressorApp()));
}

class PDFCompressorApp extends StatefulWidget {
  const PDFCompressorApp({super.key});

  @override
  State<PDFCompressorApp> createState() => _PDFCompressorAppState();
}

class _PDFCompressorAppState extends State<PDFCompressorApp> {
  File? _selectedFile;
  String _status = "No file selected";
  String _selectedQuality = 'ebook'; // Default quality

  final List<String> _qualities = ['screen', 'ebook', 'printer', 'prepress'];

  Future<void> pickPDF() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['pdf'],
    );

    if (result != null && result.files.single.path != null) {
      setState(() {
        _selectedFile = File(result.files.single.path!);
        _status = "Selected: ${result.files.single.name}";
      });
    }
  }

  Future<void> compressAndDownload() async {
    if (_selectedFile == null) {
      setState(() {
        _status = "Please select a PDF file first.";
      });
      return;
    }

    setState(() {
      _status = "Uploading...";
    });

    try {
      var request = http.MultipartRequest(
        'POST',
        Uri.parse("https://pdf-compressor-apk.onrender.com/compress"), // Replace with your server IP
      );

      request.files.add(await http.MultipartFile.fromPath('pdf', _selectedFile!.path));
      request.fields['quality'] = _selectedQuality;

      var response = await request.send();

      if (response.statusCode == 200) {
        final bytes = await response.stream.toBytes();

        final filename = response.headers['content-disposition']
                ?.split('filename=') // Extract filename
                .last
                .replaceAll('"', '') ??
            "compressed.pdf";

        // Use FileSaver to prompt the user to choose save location
        await FileSaver.instance.saveAs(
          name: filename.replaceAll('.pdf', ''),
          bytes: bytes,
          ext: "pdf",
          mimeType: MimeType.other,
        );

        setState(() {
          _status = "Downloaded successfully!";
        });
      } else {
        setState(() {
          _status = "Server error: ${response.statusCode}";
        });
      }
    } catch (e) {
      setState(() {
        _status = "Error: $e";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("PDF Compressor")),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            ElevatedButton.icon(
              icon: const Icon(Icons.upload_file),
              label: const Text("Select PDF"),
              onPressed: pickPDF,
            ),
            const SizedBox(height: 20),
            DropdownButtonFormField<String>(
              initialValue: _selectedQuality,
              items: _qualities
                  .map((q) => DropdownMenuItem(
                        value: q,
                        child: Text(q.toUpperCase()),
                      ))
                  .toList(),
              onChanged: (val) {
                if (val != null) {
                  setState(() {
                    _selectedQuality = val;
                  });
                }
              },
              decoration: const InputDecoration(
                labelText: "Compression Quality",
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: compressAndDownload,
              child: const Text("Compress & Download"),
            ),
            const SizedBox(height: 20),
            Text(
              _status,
              style: const TextStyle(fontSize: 16, color: Colors.blueGrey),
            )
          ],
        ),
      ),
    );
  }
}
