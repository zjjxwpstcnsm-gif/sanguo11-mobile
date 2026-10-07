import Foundation
import AVFoundation

// Decode every actual sample. Preserve its recorded rational timestamp;
// do not choose frames, assign a playback rate, remux or edit the source.
guard CommandLine.arguments.count == 3 else { fatalError("input.mp4 fresh-report.json") }
let input = URL(fileURLWithPath: CommandLine.arguments[1])
let output = URL(fileURLWithPath: CommandLine.arguments[2])
guard !FileManager.default.fileExists(atPath: output.path) else { fatalError("Fresh report required") }
let asset = AVURLAsset(url: input)
guard let track = asset.tracks(withMediaType: .video).first else { fatalError("Missing actual video track") }
let reader = try AVAssetReader(asset: asset)
let decoded = AVAssetReaderTrackOutput(track: track,
    outputSettings: [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA])
decoded.alwaysCopiesSampleData = false
reader.add(decoded)
guard reader.startReading() else { fatalError("Actual reader did not start") }
var frames = [[String: Any]]()
var previous: CMTime? = nil
while let sample = decoded.copyNextSampleBuffer() {
    let pts = CMSampleBufferGetPresentationTimeStamp(sample)
    guard pts.isNumeric, pts.timescale > 0,
          let pixels = CMSampleBufferGetImageBuffer(sample) else { fatalError("Invalid decoded sample") }
    if let old = previous, CMTimeCompare(pts, old) <= 0 { fatalError("Original timestamps not strictly increasing") }
    let width = CVPixelBufferGetWidth(pixels), height = CVPixelBufferGetHeight(pixels)
    guard width > 0, height > 0 else { fatalError("Invalid actual decoded dimensions") }
    frames.append(["sample": frames.count, "ptsValue": pts.value, "ptsTimescale": pts.timescale,
                   "width": width, "height": height])
    previous = pts
}
guard reader.status == .completed, !frames.isEmpty else {
    fatalError("All-sample decode incomplete: \(String(describing: reader.error))")
}
let report: [String: Any] = ["video": input.path, "decodedFrames": frames.count,
    "allSamplesDecoded": true, "timestampsStrictlyIncreasing": true,
    "originalTrackTimescale": track.naturalTimeScale, "samples": frames,
    "sourceEdited": false, "assignedFrameRate": false, "PCPixelTimingAccepted": false]
let data = try JSONSerialization.data(withJSONObject: report, options: [.prettyPrinted, .sortedKeys])
try data.write(to: output)
print("Decoded every actual sample: \(frames.count)")
