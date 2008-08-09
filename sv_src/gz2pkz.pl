
# gzipをpkzipに変換するライブラリ version 0.1

#★使用方法
# require 'gz2pkz.pl';
# $dest = &gz2pkz'convert(*gzip);

#v0.1
#gzipのflagsが0x00の時のみをサポート

#gzip
#header signature 2 bytes (0x8b1f) 
#compression method 1 byte (0x08)
#flags 1 byte
#Modification TIME 4 bytes (0 means no time stamp is available)
#eXtra FLags 1 byte
#Operating System 1 byte (0x03 == UNIX)
#data (variable size)
#crc-32 4 bytes
#uncompressed size 4 bytes

#pkzip
#http://www.pkware.com/products/enterprise/white_papers/appnote.html
#local file header signature 4 bytes (0x04034b50)
#version needed to extract 2 bytes
#general purpose bit flag 2 bytes
#compression method 2 bytes
#last mod file time 2 bytes
#last mod file date 2 bytes
#crc-32 4 bytes
#compressed size 4 bytes
#uncompressed size 4 bytes
#file name length 2 bytes
#extra field length 2 bytes
#file name (variable size)
#extra field (variable size)


package gz2pkz;

$filename = "test.txt";

sub convert {
	local(*s) = @_;

#	if($s !~ /\x1F\x8B/){	#gzipヘッダがない場合
#		$s = "\x1F\x8B\x08\x00\x00\x00\x00\x00\x00\x03" . $s
#	}

	$dest = "PK\x03\x04";
	$dest .= "\x14\x00";
	$dest .= "\x00\x00";
	$dest .= "\x08\x00";
	$dest .= substr($s, 4, 4);
	$dest .= substr($s, -8, -4);
	$dest .= pack("I",length($s) - 18);
	$dest .= substr($s, -4);
	$dest .= pack("S",length($filename));
	$dest .= "\x00\x00";
	$dest .= $filename;
	$dest .= substr($s, 10, -8);

	$dest2 = "PK\x01\x02";
	$dest2 .= "\x14\x00";
	$dest2 .= substr($dest, 4, 26);
	$dest2 .= "\x00\x00";
	$dest2 .= "\x00\x00";
	$dest2 .= "\x00\x00";
	$dest2 .= "\x00\x00\x00\x00";
	$dest2 .= "\x00\x00\x00\x00";
	$dest2 .= $filename;

	$dest3 = "PK\x05\x06";
	$dest3 .= "\x00\x00";
	$dest3 .= "\x00\x00";
	$dest3 .= "\x01\x00";
	$dest3 .= "\x01\x00";
	$dest3 .= pack("I",length($dest2));
	$dest3 .= pack("I",length($dest));
	$dest3 .= "\x00\x00";

	return $dest . $dest2 . $dest3;
}