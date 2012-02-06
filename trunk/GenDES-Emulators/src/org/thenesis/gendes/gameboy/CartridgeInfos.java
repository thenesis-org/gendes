package org.thenesis.gendes.gameboy;

import org.thenesis.gendes.Tools;

public final class CartridgeInfos {
	public static final int CARTRIDGE_HEADER_SIZE=         0x200;
    public static final int CARTRIDGE_ROM_BANK_SIZE=       0x4000;
    public static final int CARTRIDGE_RAM_BANK_SIZE=       0x2000;

    // Cartridge type features.
    public static final short
    	CTF_ROM=               0x0001,
    	CTF_RAM=               0x0002,
    	CTF_BATTERY=           0x0004,
    	CTF_RUMBLE=            0x0008,
    	CTF_RTC=               0x0010,
    	CTF_INFRARED=          0x0020;
    
    // Cartridge type controller.
    public static final short
    	CTC_NONE=              0x0000,
    	CTC_NOMBC=             0x0001,
    	CTC_MBC1=              0x0002,
    	CTC_MBC2=              0x0003,
    	CTC_MBC3=              0x0004,
    	CTC_MBC4=              0x0005,
    	CTC_MBC5=              0x0006,
    	CTC_HUC1=              0x0007,
    	CTC_HUC3=              0x0008,
    	CTC_MMM01=             0x0009,
    	CTC_CAMERA=            0x000a,
    	CTC_TAMA5=             0x000b,
    	CTC_ANY=               0x007f;    
	
    public boolean superFeaturesFlag;
    public boolean colorFeaturesFlag;
    public boolean colorOnlyFlag;

    public int controller;
    public int features;
    
    public int nbRomBanks;
    public int romSize;
    
    public int nbRamBanks;
    public int ramSize;
    
    public String gameTitle;

    public boolean headerChecksumValid;
    public boolean headerValid;
    public String errorString;
    
    private static final String CONTROLLER_TO_STRING[]={
            "None", "No MBC", "MBC1", "MBC2", "MBC3", "MBC4", "MBC5", "HUC1", "HUC3", "MMM01", "CAMERA", "TAMA5"
    };
    
    public CartridgeInfos() {
        resetInfos();
    }
    
    public CartridgeInfos(byte header[]) {
        resetInfos();
        getInfos(header);
    }
    
    public static String controllerToString(int controller) {
        return CONTROLLER_TO_STRING[controller];
    }
    
    public boolean getInfos(byte header[]) {
        resetInfos();
        errorString="";
        headerValid=true;
        
        if (((header[0x146]&0xff)==0x03) && ((header[0x14b]&0xff)==0x33)) superFeaturesFlag=true;
        
        switch (header[0x143]&0xff) {
        case 0x80: colorFeaturesFlag=true; break;
        case 0xc0: colorFeaturesFlag=true; colorOnlyFlag=true; break;
        }
        
        if (findType(header[0x0147]&0xff)) {
            errorString.concat("Invalid or unknown cartridge type.\n");
            headerValid=false;
        }

        if (findRomSize(header[0x0148]&0xff)) {
            errorString.concat("Invalid or unknown cartridge ROM size.\n");
            headerValid=false;
        }
        
        if (findRamSize(header[0x0149]&0xff)) {
            errorString.concat("Invalid or unknown cartridge RAM size.\n");
            headerValid=false;
        }
        
        // Check compatibility between the memory controller and other features.
        switch (controller) {
        case CTC_NOMBC:
            if (romSize>0x00008000) {
                errorString.concat("No MBC cartridges do not support ROM size greater than 32 KB.\n");
                headerValid=false;                
            } else if (ramSize>0x00002000) {
	            errorString.concat("No MBC cartridges do not support RAM size greater than 8 KB.\n");
	            headerValid=false;
	        }
            break;
        case CTC_MBC1:
            if (romSize>0x00200000) {
                errorString.concat("MBC1 does not support ROM size greater than 16 Mb.\n");
                headerValid=false;                
            } else if (romSize<0x00080000) {
                if (ramSize>0x00008000) {
                    errorString.concat("MBC1 does not support RAM size greater than 32 KB.\n");
                    headerValid=false;
                }
            } else {
                if (ramSize>0x00002000) {
                    errorString.concat("MBC1 does not support RAM size greater than 8 KB in 16 Mb ROM size mode.\n");
                    headerValid=false;
                }
            }
            break;
        case CTC_MBC2:
            if (romSize>0x00040000) {
                errorString.concat("MBC2 does not support ROM size greater than 2 Mb.\n");
                headerValid=false;                
            }
            if (ramSize!=0) {
                errorString.concat("MBC2 does not support external RAM.\n");
                headerValid=false;
            }
            ramSize=512; // 512*4 bits in fact.
            break;
        case CTC_MBC3:
            if (romSize>0x00200000) {
                errorString.concat("MBC3 does not support ROM size greater than 16 Mb.\n");
                headerValid=false;          
            }
            if (ramSize>0x00008000) {
                errorString.concat("MBC3 does not support RAM size greater than 32 KB.\n");
                headerValid=false;
            }
            break;
        case CTC_MBC5:
            if (romSize>0x00800000) {
                errorString.concat("MBC5 does not support ROM size greater than 64 Mb.\n");
                headerValid=false;          
            }
            if (ramSize>0x00020000) {
                errorString.concat("MBC5 does not support RAM size greater than 128 KB.\n");
                headerValid=false;
            }
            break;
        }
        
        headerChecksumValid=verifyHeaderChecksum(header);
        if (!headerChecksumValid) {
            errorString.concat("Invalid header checksum.\n");
            headerValid=false;
        }
        
        // Cartridge title.
        {
            char buffer[]=new char[16];
            int bufferLength=0;
            for (int i=0x0134; i<0x0144; i++) {
                char c=(char)(header[i]&0x00ff);
                if (!Tools.isPrintable(c)) c=' ';
                buffer[bufferLength++]=c;
            }
            gameTitle=(bufferLength>0) ? String.valueOf(buffer, 0, bufferLength) : "";
        }
        
        return !headerValid;
    }
    
    public void resetInfos() {
        superFeaturesFlag=false;
        colorFeaturesFlag=false;
        colorOnlyFlag=false;
        controller=CTC_NONE; features=0;
        nbRomBanks=0; romSize=0;
        nbRamBanks=0; ramSize=0;
        gameTitle="";
        headerValid=false;
        errorString="No cartridge.\n";        
    }
        
    // Find the cartridge type.
    private boolean findType(int typeCode) {
        int n=CARTRIDGE_TYPE_TABLE.length;
        for (int i=0; i<n; i++) {
            if (CARTRIDGE_TYPE_TABLE[i][0]==typeCode) {
                controller=CARTRIDGE_TYPE_TABLE[i][1];
                features=CARTRIDGE_TYPE_TABLE[i][2];
                return false;
            }
        }
        return true;
    }
    
    // Find the number of ROM banks and the ROM size.
    private boolean findRomSize(int romSizeCode) {
        int n=CARTRIDGE_ROM_SIZE_TABLE.length;
        for (int i=0; i<n; i++) {
            if (CARTRIDGE_ROM_SIZE_TABLE[i][0]==romSizeCode) {
                nbRomBanks=CARTRIDGE_ROM_SIZE_TABLE[i][1];
                romSize=nbRomBanks*CARTRIDGE_ROM_BANK_SIZE;
                return false;
            }
        }
        return true;
    }
    
    // Find the number of RAM banks and the RAM size.
    private boolean findRamSize(int ramSizeCode) {
        int n=CARTRIDGE_RAM_SIZE_TABLE.length;
        for (int i=0; i<n; i++) {
            if (CARTRIDGE_RAM_SIZE_TABLE[i][0]==ramSizeCode) {
                nbRamBanks=CARTRIDGE_RAM_SIZE_TABLE[i][1];
                ramSize=CARTRIDGE_RAM_SIZE_TABLE[i][2];
                return false;
            }
        }
        return true;
    }
    
    // Verify the cartridge ROM header checksum.
    public static boolean verifyHeaderChecksum(byte header[]) {
        int checksum=header[0x014d]&0xff;
        int total=0;
        for (int i=0x0134; i<=0x014c; i++) total-=header[i]+1;
        total&=0xff;
        return checksum==total;
    }
    
    // Verify the cartridge ROM checksum.
    public static boolean verifyChecksum(byte romData[]) {
        int csh=romData[0x14E]&0xff, csl=romData[0x14F]&0xff;
        int checkSum=(csh<<8) | csl;
        int total=-csh-csl;
        for (int r=0; r<romData.length; r++) total+=romData[r]&0xff;
        total&=0xffff;
        return checkSum==total;
    }
    
    // Cartridge type table.
    private static final short CARTRIDGE_TYPE_TABLE[][]={
        {   0x00, CTC_NOMBC, CTF_ROM },
        {   0x08, CTC_NOMBC, CTF_ROM | CTF_RAM },
        {   0x09, CTC_NOMBC, CTF_ROM | CTF_RAM | CTF_BATTERY },
        
        {   0x01, CTC_MBC1,  CTF_ROM },
        {   0x02, CTC_MBC1,  CTF_ROM | CTF_RAM },
        {   0x03, CTC_MBC1,  CTF_ROM | CTF_RAM | CTF_BATTERY },
        
        {   0x05, CTC_MBC2,  CTF_ROM },
        {   0x06, CTC_MBC2,  CTF_ROM | CTF_BATTERY },
        
        {   0x0f, CTC_MBC3,  CTF_ROM | CTF_BATTERY | CTF_RTC },
        {   0x10, CTC_MBC3,  CTF_ROM | CTF_RAM | CTF_BATTERY | CTF_RTC },
        {   0x11, CTC_MBC3,  CTF_ROM },
        {   0x12, CTC_MBC3,  CTF_ROM | CTF_RAM },
        {   0x13, CTC_MBC3,  CTF_ROM | CTF_RAM | CTF_BATTERY },
/*
        {   0x15, CTC_MBC4,  CTF_ROM },
        {   0x16, CTC_MBC4,  CTF_ROM | CTF_RAM },
        {   0x17, CTC_MBC4,  CTF_ROM | CTF_RAM | CTF_BATTERY },
*/
        {   0x19, CTC_MBC5,  CTF_ROM },
        {   0x1a, CTC_MBC5,  CTF_ROM | CTF_RAM },
        {   0x1b, CTC_MBC5,  CTF_ROM | CTF_RAM | CTF_BATTERY },
        {   0x1c, CTC_MBC5,  CTF_ROM | CTF_RUMBLE },
        {   0x1d, CTC_MBC5,  CTF_ROM | CTF_RAM | CTF_RUMBLE },
        {   0x1e, CTC_MBC5,  CTF_ROM | CTF_RAM | CTF_BATTERY | CTF_RUMBLE },
/*
        {   0x0b, CTC_MMM01, CTF_ROM },
        {   0x0c, CTC_MMM01, CTF_ROM | CTF_RAM },
        {   0x0d, CTC_MMM01, CTF_ROM | CTF_RAM | CTF_BATTERY },
*/
/*
        {   0x1f, CTC_CAMERA, 0 },
        {   0xfd, CTC_TAMA5,  0 },
        {   0xfe, CTC_HUC3, CTF_ROM },
        {   0xff, CTC_HUC1, CTF_ROM | CTF_RAM | CTF_BATTERY }
*/
    };

    // Cartridge ROM size table.
    private static final int[][] CARTRIDGE_ROM_SIZE_TABLE={
        { 0x00, 2 },
        { 0x01, 4 },
        { 0x02, 8 },
        { 0x03, 16 },
        { 0x04, 32 },
        { 0x05, 64 },
        { 0x06, 128 },
        { 0x07, 256 },
        { 0x08, 512 },
        { 0x52, 72 },
        { 0x53, 80 },
        { 0x54, 96 }
    };

    // Cartridge RAM size table.
    private static final int[][] CARTRIDGE_RAM_SIZE_TABLE={
        {   0x00, 0,  0x00000000 }, // No RAM.
        {   0x01, 1,  0x00000800 }, // 2KB, 1 bank.
        {   0x02, 1,  0x00002000 }, // 8KB, 1 bank.
        {   0x03, 4,  0x00008000 }, // 32KB, 4 banks.
        {   0x06, 16, 0x00020000 } // 128KB, 16 banks.
    };
}
