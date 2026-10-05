import Foundation
import AVFoundation
import CoreVideo

// Encode only captured original pixels, preserving measured variable intervals.
let args=CommandLine.arguments
guard args.count==3 else {fatalError("frames.json output.mp4")}
let manifest=URL(fileURLWithPath:args[1]).standardizedFileURL
let destination=URL(fileURLWithPath:args[2]).standardizedFileURL
let source=try JSONSerialization.jsonObject(with:Data(contentsOf:manifest)) as! [String:Any]
let frames=source["frames"] as! [[String:Any]]
guard frames.count>=2, !FileManager.default.fileExists(atPath:destination.path) else {fatalError("Need at least two frames and a fresh output path")}
let width=frames[0]["width"] as! Int, height=frames[0]["height"] as! Int
let writer=try AVAssetWriter(outputURL:destination,fileType:.mp4)
let input=AVAssetWriterInput(mediaType:.video,outputSettings:[AVVideoCodecKey:AVVideoCodecType.h264,AVVideoWidthKey:width,AVVideoHeightKey:height,
 AVVideoCompressionPropertiesKey:[AVVideoAverageBitRateKey:16000000,AVVideoMaxKeyFrameIntervalKey:10]])
input.expectsMediaDataInRealTime=false
let attributes:[String:Any]=[kCVPixelBufferPixelFormatTypeKey as String:kCVPixelFormatType_32BGRA,
 kCVPixelBufferWidthKey as String:width,kCVPixelBufferHeightKey as String:height,
 kCVPixelBufferIOSurfacePropertiesKey as String:[:]]
let adaptor=AVAssetWriterInputPixelBufferAdaptor(assetWriterInput:input,sourcePixelBufferAttributes:attributes)
guard writer.canAdd(input) else {fatalError("Cannot add source video")};writer.add(input)
guard writer.startWriting() else {fatalError("Cannot start writing: \(String(describing:writer.error))")}
writer.startSession(atSourceTime:.zero)
let firstTime=frames[0]["time_seconds"] as! Double
for (index,frame) in frames.enumerated() {
 guard frame["width"] as! Int==width, frame["height"] as! Int==height else {fatalError("Changed original buffer size")}
 let path=manifest.deletingLastPathComponent().appendingPathComponent(frame["file"] as! String)
 let bytes=try Data(contentsOf:path)
 guard bytes.count==54+width*height*4,bytes[0]==66,bytes[1]==77 else {fatalError("Invalid original BMP")}
 var buffer:CVPixelBuffer?
 guard CVPixelBufferCreate(kCFAllocatorDefault,width,height,kCVPixelFormatType_32BGRA,attributes as CFDictionary,&buffer)==kCVReturnSuccess,let pixel=buffer else {fatalError("Buffer allocation")}
 CVPixelBufferLockBaseAddress(pixel,[])
 let stride=CVPixelBufferGetBytesPerRow(pixel);let base=CVPixelBufferGetBaseAddress(pixel)!
 bytes.withUnsafeBytes { raw in
  for row in 0..<height {
   let out=base.advanced(by:row*stride);memcpy(out,raw.baseAddress!.advanced(by:54+row*width*4),width*4)
   let channel=out.assumingMemoryBound(to:UInt8.self)
   for x in 0..<width {channel[x*4+3]=255} // Video is RGB; preserve actual B/G/R.
  }
 }
 CVPixelBufferUnlockBaseAddress(pixel,[])
 let deadline=Date().addingTimeInterval(20)
 while !input.isReadyForMoreMediaData {
  if Date()>deadline || writer.status == .failed {fatalError("Encoder blocked: \(String(describing:writer.error))")};Thread.sleep(forTimeInterval:0.01)
 }
 let pts=CMTime(seconds:(frame["time_seconds"] as! Double)-firstTime,preferredTimescale:1000000)
 guard adaptor.append(pixel,withPresentationTime:pts) else {fatalError("Append source frame \(index): \(String(describing:writer.error))")}
}
let end=(frames.last!["time_seconds"] as! Double)-firstTime+1/(source["requested_hz"] as! Double)
writer.endSession(atSourceTime:CMTime(seconds:end,preferredTimescale:1000000));input.markAsFinished()
let finished=DispatchSemaphore(value:0);writer.finishWriting{finished.signal()}
guard finished.wait(timeout:.now()+60) == .success,writer.status == .completed else {fatalError("Incomplete encoding: \(String(describing:writer.error))")}
print("Encoded \(frames.count) original frames, measured duration \(end), \(destination.path)")
