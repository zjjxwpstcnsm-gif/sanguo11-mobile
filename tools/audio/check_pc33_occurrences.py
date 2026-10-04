#!/usr/bin/env python3
"""Fit distinct original HUD33 occurrences in actual mixer PCM, including overlapping playback.

One waveform under two cue aliases is never counted twice. No play-return or log-counter acceptance.
"""
import argparse
import hashlib
import json
from pathlib import Path
import wave
import numpy as np

ROOT=Path(__file__).resolve().parents[2]
def read(path):
    with wave.open(str(path)) as stream:
        if stream.getsampwidth()!=2:raise ValueError('Expected original/native16-bit PCM')
        rate=stream.getframerate()
        signal=np.frombuffer(stream.readframes(stream.getnframes()),dtype='<i2').astype(np.float64)
        return rate,signal.reshape(-1,stream.getnchannels()).mean(axis=1)/32768
def verify(capture,minimum):
    source=ROOT/'app/src/main/assets/audio/pc/technique-33.wav'
    rate,signal=read(capture);source_rate,template=read(source)
    if rate!=source_rate:raise ValueError('Unexamined sample-rate conversion')
    tap=np.arange(-32,33);kernel=np.sinc(tap*.1)*np.hamming(len(tap));kernel/=kernel.sum()
    wide_signal=np.convolve(signal,kernel,mode='same');wide_template=np.convolve(template,kernel,mode='same')
    signal=wide_signal[::8];template=wide_template[::8]
    length=len(template);energy=float(template@template)
    if len(signal)<length or not 1<=minimum<=32:raise ValueError('Insufficient interval/count')
    positions=[];initial_scores=[];residual=signal.copy();amplitudes=[]
    fft_size=1<<(len(signal)+length-2).bit_length()
    spectrum=np.fft.rfft(template[::-1],fft_size)
    for _ in range(minimum):
        cross=np.fft.irfft(np.fft.rfft(residual,fft_size)*spectrum,fft_size)[length-1:len(signal)]
        sums=np.r_[0,np.cumsum(residual*residual)]
        scores=np.abs(cross)/np.sqrt(np.maximum(1e-20,(sums[length:]-sums[:-length])*energy))
        for position in positions:
            radius=int(rate/8*.15);scores[max(0,position-radius):position+radius+1]=0
        at=int(np.argmax(scores));score=float(scores[at])
        # An overlapped original sample can have a low first independent correlation.
        # Acceptance below is the simultaneous reconstruction of the entire PCM union.
        if score<.60:raise AssertionError(('No distinct waveform candidate',score,len(positions)))
        positions.append(at);initial_scores.append(score)
        basis=np.zeros((len(signal),len(positions)))
        for column,position in enumerate(positions):basis[position:position+length,column]=template
        amplitudes=np.linalg.lstsq(basis,signal,rcond=None)[0]
        if np.any(amplitudes<.02):raise AssertionError(('Not audible positive original waveform gains',amplitudes))
        residual=signal-basis@amplitudes
    # Overlap shifts the independent correlation peak slightly. Refine starts
    # against the simultaneous fit rather than relaxing the PCM acceptance threshold.
    # Keep original-rate phase for the final fit. Decimation can move a start by
    # a fraction of one output sample and distort a strongly periodic waveform.
    signal=wide_signal;template=wide_template;length=len(template);positions=[position*8 for position in positions]
    wide_fft=1<<(len(signal)+length-2).bit_length()
    wide_cross=np.fft.irfft(np.fft.rfft(signal,wide_fft)*np.fft.rfft(template[::-1],wide_fft),wide_fft)[length-1:len(signal)]
    auto_fft=1<<(2*length-2).bit_length();template_fft=np.fft.rfft(template,auto_fft)
    autocorrelation=np.fft.irfft(template_fft*np.conj(template_fft),auto_fft)[:length]
    total_energy=float(signal@signal)
    def fit(starts):
        gram=np.zeros((len(starts),len(starts)));dots=np.zeros(len(starts))
        for i,start in enumerate(starts):
            dots[i]=wide_cross[start]
            for j,other in enumerate(starts):
                shift=abs(start-other);gram[i,j]=autocorrelation[shift] if shift<length else 0
        gains=np.linalg.lstsq(gram,dots,rcond=None)[0]
        return float(total_energy-dots@gains),gains
    for _ in range(2):
        for index in range(len(positions)):
            best=positions.copy();error,_=fit(best)
            for candidate in range(max(0,positions[index]-768),min(len(signal)-length,positions[index]+768)+1):
                trial=positions.copy();trial[index]=candidate
                if any(abs(candidate-other)<int(rate*.15) for j,other in enumerate(positions) if j!=index):continue
                loss,_=fit(trial)
                if loss<error:best=trial;error=loss
            positions=best
    _,amplitudes=fit(positions)
    if np.any(amplitudes<.02):raise AssertionError(('Refined nonpositive/inaudible waveform gains',amplitudes))
    basis=np.zeros((len(signal),len(positions)))
    for column,position in enumerate(positions):basis[position:position+length,column]=template
    residual=signal-basis@amplitudes
    fitted=basis@amplitudes;mask=np.any(basis!=0,axis=1)
    explained=1-float(residual[mask]@residual[mask])/float(signal[mask]@signal[mask])
    correlation=float(fitted[mask]@signal[mask])/float(np.linalg.norm(fitted[mask])*np.linalg.norm(signal[mask]))
    if explained<.97 or correlation<.985:raise AssertionError(('PCM union not explained by original samples',explained,correlation))
    return dict(status='PASS',scope='Distinct original HUD33 waveforms in actual emulator PCM; no ARM speaker evidence',
        captureSha256=hashlib.sha256(capture.read_bytes()).hexdigest(),sourceSha256=hashlib.sha256(source.read_bytes()).hexdigest(),
        uniqueOriginalSamples=1,occurrences=minimum,sampleRate=rate,explainedEnergy=explained,unionCorrelation=correlation,
        starts=[dict(seconds=position/rate,gain=float(gain),candidateCorrelation=score)
            for position,gain,score in sorted(zip(positions,amplitudes,initial_scores))])
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('capture',type=Path)
    parser.add_argument('--minimum',type=int,required=True);parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args();report=verify(args.capture,args.minimum)
    args.output.write_text(json.dumps(report,indent=2)+'\n');print('PASS original HUD33 distinct mixer occurrences=',report['occurrences'])
