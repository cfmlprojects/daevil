@param Object... args
!{String zipFile = args[0].toString(); }
!{String unzipToDir = args[1].toString(); }
7za e "${zipFile}" $("-o" + "${unzipToDir}")