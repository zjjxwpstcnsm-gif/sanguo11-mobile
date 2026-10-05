import Foundation
import AVFoundation
import AppKit

// Sequential actual sample decoding avoids choosing a convenient frame or
// treating requested timestamps as recorded evidence. Source pixels unchanged.
let args = CommandLine.arguments
guard args.count == 4, let limit = Double(args[3]), limit > 0, limit <= 60 else {
    fatalError("video new-output-directory first-seconds(0..60)")
}
let input = URL(fileURLWithPath: args[1]).standardizedFileURL
let output = URL(fileURLWithPath: args[2]).standardizedFileURL
guard !FileManager.default.fileExists(atPath: output.path) else {
    fatalError("Use a fresh output directory; preserve previous evidence")
}
try FileManager.default.createDirectory(at: output, withIntermediateDirectories: true)
let asset = AVURLAsset(url: input)
guard let track = asset.tracks(withMediaType: .video).first else { fatalError("No video track") }
let reader = try AVAssetReader(asset: asset)
let decoded = AVAssetReaderTrackOutput(track: track, outputSettings: [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA])
decoded.alwaysCopiesSampleData = false
reader.add(decoded)
guard reader.startReading() else { fatalError("Reader start: \(String(describing: reader.error))") }
let context = CIContext(options: nil)
var rows = [[String: Any]]()
while let sample = decoded.copyNextSampleBuffer() {
    let pts = CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sample))
    if pts > limit { break }
    guard let pixels = CMSampleBufferGetImageBuffer(sample) else { fatalError("Missing decoded pixels") }
    let ci = CIImage(cvPixelBuffer: pixels).transformed(by: track.preferredTransform)
    guard let frame = context.createCGImage(ci, from: ci.extent) else { fatalError("Missing frame") }
    let path = output.appendingPathComponent(String(format: "sample-%04d.png", rows.count))
    guard let png = NSBitmapImageRep(cgImage: frame).representation(using: .png, properties: [:]) else { fatalError("PNG encode") }
    try png.write(to: path)
    rows.append(["sample": rows.count, "actual_seconds": pts, "width": frame.width, "height": frame.height, "path": path.path])
}
if reader.status == .failed { throw reader.error! }
reader.cancelReading()
let report: [String: Any] = ["video": input.path, "decoded_sample_count": rows.count, "scan_seconds": limit, "frames": rows, "PC_match_accepted": false]
let json = try JSONSerialization.data(withJSONObject: report, options: [.prettyPrinted, .sortedKeys])
try json.write(to: output.appendingPathComponent("decoded-samples.json"))
print("Decoded \(rows.count) actual samples through \(limit)s into \(output.path)")
