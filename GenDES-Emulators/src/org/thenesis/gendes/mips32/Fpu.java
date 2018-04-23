package org.thenesis.gendes.mips32;

import static org.thenesis.gendes.mips32.CpuConstants.FPCR_FCCR;
import static org.thenesis.gendes.mips32.CpuConstants.FPCR_FCSR;
import static org.thenesis.gendes.mips32.CpuConstants.FPCR_FIR;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FNX_MADDS;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_ABS;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_ADD;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_CVT_D;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_CVT_S;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_CVT_W;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_C_EQ;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_C_LE;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_C_LT;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_C_ULT;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_DIV;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_MOV;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_MUL;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_NEG;
import static org.thenesis.gendes.mips32.CpuConstants.FP_FN_SUB;
import static org.thenesis.gendes.mips32.CpuConstants.FP_RS_BC1;
import static org.thenesis.gendes.mips32.CpuConstants.FP_RS_CFC1;
import static org.thenesis.gendes.mips32.CpuConstants.FP_RS_CTC1;
import static org.thenesis.gendes.mips32.CpuConstants.FP_RS_D;
import static org.thenesis.gendes.mips32.CpuConstants.FP_RS_MFC1;
import static org.thenesis.gendes.mips32.CpuConstants.FP_RS_MTC1;
import static org.thenesis.gendes.mips32.CpuConstants.FP_RS_S;
import static org.thenesis.gendes.mips32.CpuConstants.FP_RS_W;
import static org.thenesis.gendes.mips32.CpuFunctions.fccrFcc;
import static org.thenesis.gendes.mips32.CpuFunctions.fd;
import static org.thenesis.gendes.mips32.CpuFunctions.fn;
import static org.thenesis.gendes.mips32.CpuFunctions.fpcc;
import static org.thenesis.gendes.mips32.CpuFunctions.fpnd;
import static org.thenesis.gendes.mips32.CpuFunctions.fptf;
import static org.thenesis.gendes.mips32.CpuFunctions.fr;
import static org.thenesis.gendes.mips32.CpuFunctions.fs;
import static org.thenesis.gendes.mips32.CpuFunctions.ft;
import static org.thenesis.gendes.mips32.CpuFunctions.loadSingle;
import static org.thenesis.gendes.mips32.CpuFunctions.rs;
import static org.thenesis.gendes.mips32.CpuFunctions.rt;
import static org.thenesis.gendes.mips32.CpuFunctions.storeDouble;
import static org.thenesis.gendes.mips32.CpuFunctions.storeSingle;
import static org.thenesis.gendes.mips32.InstructionUtil.opString;

public class Fpu {
	
	/** coprocessor 1 registers (longs/doubles in consecutive registers) */
	private final int[] fpReg = new int[32];
	private final int[] fpControlReg = new int[32];
	private final Cpu cpu;
	
	private FpRound roundingMode = FpRound.NONE;
	
	public Fpu (Cpu cpu) {
		this.cpu = cpu;

		// support S, D, W, L (unlike the 4kc...)
		fpControlReg[FPCR_FIR] = (1 << 16) | (1 << 17) | (1 << 20) | (1 << 21) | (1 << 8);
	}

	public final int getFpRegister (int n) {
		return fpReg[n];
	}
	
	public final void setFpRegister (int n, int value) {
		fpReg[n] = value;
	}
	
	public final double getFpRegister (int n, FpFormat fmt) {
		return fmt.load(fpReg, n);
	}

	public final int[] getFpControlReg () {
		return fpControlReg;
	}
	
	public final void execFpuRs (final int isn) {
		final int rs = rs(isn);
		final int rt = rt(isn);
		final int fs = fs(isn);
		
		switch (rs) {
			case FP_RS_MFC1:
				cpu.setRegister(rt, fpReg[fs]);
				return;
				
			case FP_RS_MTC1:
				fpReg[fs] = cpu.getRegister(rt);
				return;
				
			case FP_RS_S:
				execFpuFn(isn, FpFormat.SINGLE);
				return;
				
			case FP_RS_D:
				execFpuFn(isn, FpFormat.DOUBLE);
				return;
				
			case FP_RS_W:
				execFpuFnWord(isn);
				return;
				
			case FP_RS_BC1:
				if (fptf(isn) == fccrFcc(fpControlReg, fpcc(isn))) {
					cpu.execBranch(isn);
				} else if (fpnd(isn)) {
					throw new RuntimeException();
				}
				return;
				
			case FP_RS_CFC1:
				execFpuCopyFrom(isn);
				return;
				
			case FP_RS_CTC1:
				execFpuCopyTo(isn);
				return;
				
			default:
				throw new RuntimeException("invalid fpu rs " + opString(rs));
		}
		
	}
	
	private final void execFpuCopyFrom (final int isn) {
		final int rt = rt(isn);
		final int fs = fs(isn);
		
		switch (fs) {
			case FPCR_FCSR:
			case FPCR_FCCR:
			case FPCR_FIR:
				cpu.setRegister(rt, fpControlReg[fs]);
				return;
				
			default:
				throw new RuntimeException("read unimplemented fp control register " + fs);
		}
	}
	
	private final void execFpuCopyTo (final int isn) {
		final int rtValue = cpu.getRegister(rt(isn));
		final int fs = fs(isn);
		
		switch (fs) {
			case FPCR_FCSR:
				if ((rtValue & ~0x3) != 0) {
					throw new RuntimeException("unknown fcsr %x\n");
				}
				break;
				
			default:
				throw new RuntimeException("write unimplemented fp control register " + fs + ", " + Integer.toHexString(rtValue));
		}
		
		fpControlReg[fs] = rtValue;
		roundingMode = FpRound.getInstance(fpControlReg[FPCR_FCSR]);
	}
	
	private final void execFpuFn (final int isn, final FpFormat fmt) {
		final int[] fpReg = this.fpReg;
		final int fs = fs(isn);
		final int ft = ft(isn);
		final int fd = fd(isn);
		final int fn = fn(isn);
		
		switch (fn) {
			case FP_FN_ADD:
				fmt.store(fpReg, fd, roundingMode.round(fmt.load(fpReg, fs) + fmt.load(fpReg, ft)));
				return;
			case FP_FN_SUB:
				fmt.store(fpReg, fd, roundingMode.round(fmt.load(fpReg, fs) - fmt.load(fpReg, ft)));
				return;
			case FP_FN_MUL:
				fmt.store(fpReg, fd, roundingMode.round(fmt.load(fpReg, fs) * fmt.load(fpReg, ft)));
				return;
			case FP_FN_DIV:
				fmt.store(fpReg, fd, roundingMode.round(fmt.load(fpReg, fs) / fmt.load(fpReg, ft)));
				return;
			case FP_FN_ABS:
				fmt.store(fpReg, fd, StrictMath.abs(fmt.load(fpReg, fs)));
				return;
			case FP_FN_MOV:
				fmt.store(fpReg, fd, fmt.load(fpReg, fs));
				return;
			case FP_FN_NEG:
				fmt.store(fpReg, fd, 0.0 - fmt.load(fpReg, fs));
				return;
			case FP_FN_CVT_S:
				storeSingle(fpReg, fd, (float) roundingMode.round(fmt.load(fpReg, fs)));
				return;
			case FP_FN_CVT_D:
				storeDouble(fpReg, fd, roundingMode.round(fmt.load(fpReg, fs)));
				return;
			case FP_FN_CVT_W:
				fpReg[fd] = (int) roundingMode.round(fmt.load(fpReg, fs));
				return;
			case FP_FN_C_ULT: {
				final double fsValue = fmt.load(fpReg, fs);
				final double ftValue = fmt.load(fpReg, ft);
				setFpCondition(fpcc(isn), Double.isNaN(fsValue) || Double.isNaN(ftValue) || fsValue < ftValue);
				return;
			}
			case FP_FN_C_EQ:
				setFpCondition(fpcc(isn), fmt.load(fpReg, fs) == fmt.load(fpReg, ft));
				return;
			case FP_FN_C_LT:
				setFpCondition(fpcc(isn), fmt.load(fpReg, fs) < fmt.load(fpReg, ft));
				return;
			case FP_FN_C_LE:
				setFpCondition(fpcc(isn), fmt.load(fpReg, fs) <= fmt.load(fpReg, ft));
				return;
			default:
				throw new RuntimeException("invalid fpu fn " + opString(fn));
		}
	}
	
	private final void execFpuFnWord (final int isn) {
		final int fn = fn(isn);
		final int fs = fs(isn);
		final int fd = fd(isn);
		
		switch (fn) {
			case FP_FN_CVT_D:
				storeDouble(fpReg, fd, fpReg[fs]);
				return;
			case FP_FN_CVT_S:
				storeSingle(fpReg, fd, fpReg[fs]);
				return;
			default:
				throw new RuntimeException("invalid fpu fn word " + opString(fn));
		}
	}
	
	public final void execFpuFnX (final int isn) {
		final int fn = fn(isn);
		final int fr = fr(isn);
		final int ft = ft(isn);
		final int fs = fs(isn);
		final int fd = fd(isn);
		
		switch (fn) {
			case FP_FNX_MADDS:
				storeSingle(fpReg, fd, loadSingle(fpReg, fs) * loadSingle(fpReg, ft) + loadSingle(fpReg, fr));
				return;
			default:
				throw new RuntimeException("invalid fpu fnx " + opString(fn));
		}
	}
	
	private final void setFpCondition (final int cc, final boolean cond) {
		if (cc >= 0 && cc < 8) {
			// oh my god
			final int ccMask = 1 << cc;
			final int csMask = 1 << (cc == 0 ? 23 : cc + 25);
			int fccr = fpControlReg[FPCR_FCCR];
			int fcsr = fpControlReg[FPCR_FCSR];
			if (cond) {
				// set the bits
				fccr = fccr | ccMask;
				fcsr = fcsr | csMask;
			} else {
				// clear the bits
				fccr = fccr & ~ccMask;
				fcsr = fcsr & ~csMask;
			}
			fpControlReg[FPCR_FCCR] = fccr;
			fpControlReg[FPCR_FCSR] = fcsr;
			
		} else {
			throw new IllegalArgumentException("invalid fpu cc " + cc);
		}
	}
	
}
