import Foundation
import AVFoundation
import AppKit

// Decode actual saved recording pixels; this is not PC reference acceptance.
let arguments=CommandLine.arguments
guard arguments.count>=3 else {fatalError("video output-directory [seconds...]")}
let file=URL(fileURLWithPath:arguments[1]).standardizedFileURL
let output=URL(fileURLWithPath:arguments[2]).standardizedFileURL
guard !FileManager.default.fileExists(atPath:output.path) else {fatalError("Use a fresh output directory; preserve previous evidence")}
try FileManager.default.createDirectory(at:output,withIntermediateDirectories:true)
let seconds=(arguments.count>3 ? arguments.dropFirst(3).map{Double($0)!} : [5.0,45.0,80.0]).sorted()
guard !seconds.isEmpty, seconds.allSatisfy({$0.isFinite && $0 >= 0}) else {fatalError("Invalid requested times")}
let asset=AVURLAsset(url:file)
guard let track=asset.tracks(withMediaType:.video).first else {fatalError("No video track")}
let reader=try AVAssetReader(asset:asset)
let decoded=AVAssetReaderTrackOutput(track:track,outputSettings:[kCVPixelBufferPixelFormatTypeKey as String:kCVPixelFormatType_32BGRA])
decoded.alwaysCopiesSampleData=false;reader.add(decoded)
guard reader.startReading() else {fatalError("Reader failed to start")}
// This environment's ImageGenerator random seeks returned stale map pixels
// for H264 samples that sequential decoding proves contain original cut-ins.
// Decode in presentation order and select the nearest actual sample instead.
let context=CIContext(options:nil)
var previous:CIImage?,previousTime=0.0,index=0
var records=[[String:Any]]()
func writeFrame(_ ci:CIImage,_ actual:Double,_ second:Double)throws {
    guard let frame=context.createCGImage(ci,from:ci.extent) else {fatalError("Missing decoded frame")}
    let bitmap=NSBitmapImageRep(cgImage:frame)
    // Preserve distinct requested fractional times when examining a brief
    // original cut-in. Integer evidence filenames remain compatible.
    let label=second==floor(second) ? String(Int(second)) : String(second).replacingOccurrences(of:".",with:"p")
    let target=output.appendingPathComponent("video-frame-\(label).png")
    try bitmap.representation(using:.png,properties:[:])!.write(to:target)
    records.append(["requested_seconds":second,"actual_seconds":actual,"width":frame.width,"height":frame.height,"path":target.path])
}
while index < seconds.count,let sample=decoded.copyNextSampleBuffer() {
    let pts=CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sample))
    guard let pixels=CMSampleBufferGetImageBuffer(sample) else {fatalError("Missing sample pixels")}
    let current=CIImage(cvPixelBuffer:pixels).transformed(by:track.preferredTransform)
    while index < seconds.count && pts >= seconds[index] {
        let request=seconds[index]
        if let prior=previous,request-previousTime <= pts-request {try writeFrame(prior,previousTime,request)}
        else {try writeFrame(current,pts,request)}
        index+=1
    }
    previous=current;previousTime=pts
}
if reader.status == .failed {throw reader.error!}
guard index == seconds.count else {fatalError("Requested sample beyond recorded extent; existing evidence retained")}
reader.cancelReading()
let data=try JSONSerialization.data(withJSONObject:["video":file.path,"decoder":"AVAssetReader sequential actual samples / nearest PTS","frames":records,"PC_match_accepted":false],options:[.prettyPrinted,.sortedKeys])
try data.write(to:output.appendingPathComponent("decoded-frames.json"))
print(String(data:data,encoding:.utf8)!)
