function Uint8Array () {};
function ArrayBuffer() {};
function Int8Array() {};
function DataView() {};

DataView.prototype.getInt16 = function() {};
DataView.prototype.setInt16 = function() {};
DataView.prototype.getUint16 = function() {};
DataView.prototype.setUint16 = function() {};
DataView.prototype.getInt32 = function() {};
DataView.prototype.setInt32 = function() {};
DataView.prototype.getUint32 = function() {};
DataView.prototype.setUint32 = function() {};

DataView.prototype.getFloat32 = function() {};
DataView.prototype.setFloat32 = function() {};
DataView.prototype.getFloat64 = function() {};
DataView.prototype.setFloat64 = function() {};

DataView.prototype.getInt8 = function() {};
DataView.prototype.setInt8 = function() {};
DataView.prototype.getUint8 = function() {};
DataView.prototype.setUint8 = function() {};

DataView.prototype.byteOffset;
DataView.prototype.byteLength;
DataView.prototype.buffer;

var TopLevel = {
    "length" : function () {},
    "subarray": function () {},
    "charCodeAt": function() {},
    "fromCharCode": function() {},
    "apply": function() {}
};
