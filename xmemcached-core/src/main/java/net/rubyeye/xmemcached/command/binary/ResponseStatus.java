/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.command.binary;

/**
 * Binary protocol response status.
 * 
 * @author dennis
 * 
 */
public enum ResponseStatus {

	NO_ERROR {
		@Override
		public short fieldValue() {
			return 0x0000;
		}

		@Override
		public String errorMessage() {
			return "No error";
		}
	},
	KEY_NOT_FOUND {
		@Override
		public short fieldValue() {
			return 0x0001;
		}

		@Override
		public String errorMessage() {
			return "Key is not found.";
		}
	},
	KEY_EXISTS {
		@Override
		public short fieldValue() {
			return 0x0002;
		}

		@Override
		public String errorMessage() {
			return "Key is already existed.";
		}
	},
	VALUE_TOO_BIG {
		@Override
		public short fieldValue() {
			return 0x0003;
		}

		@Override
		public String errorMessage() {
			return "Value is too big.";
		}
	},
	INVALID_ARGUMENTS {
		@Override
		public short fieldValue() {
			return 0x0004;
		}

		@Override
		public String errorMessage() {
			return "Invalid arguments.";
		}
	},
	ITEM_NOT_STORED {
		@Override
		public short fieldValue() {
			return 0x0005;
		}

		@Override
		public String errorMessage() {
			return "Item is not stored.";
		}
	},
	INC_DEC_NON_NUM {
		@Override
		public short fieldValue() {
			return 0x0006;
		}

		@Override
		public String errorMessage() {
			return "Incr/Decr on non-numeric value.";
		}
	},
	BELONGS_TO_ANOTHER_SRV {
		@Override
		public short fieldValue() {
			return 0x0007;
		}

		@Override
		public String errorMessage() {
			return "The vbucket belongs to another server.";
		}
	},
	AUTH_ERROR {
		@Override
		public short fieldValue() {
			return 0x0008;
		}

		@Override
		public String errorMessage() {
			return "Authentication error .";
		}
	},
	AUTH_CONTINUE {
		@Override
		public short fieldValue() {
			return 0x0009;
		}

		@Override
		public String errorMessage() {
			return "Authentication continue .";
		}
	},
	UNKNOWN_COMMAND {
		@Override
		public short fieldValue() {
			return 0x0081;
		}

		@Override
		public String errorMessage() {
			return "Unknown command error.";
		}
	},
	OUT_OF_MEMORY {
		@Override
		public short fieldValue() {
			return 0x0082;
		}

		@Override
		public String errorMessage() {
			return "Out of memory .";
		}
	},
	NOT_SUPPORTED {
		@Override
		public short fieldValue() {
			return 0x0083;
		}

		@Override
		public String errorMessage() {
			return "Not supported .";
		}
	},
	INTERNAL_ERROR {
		@Override
		public short fieldValue() {
			return 0x0084;
		}

		@Override
		public String errorMessage() {
			return "Internal error .";
		}
	},
	BUSY {
		@Override
		public short fieldValue() {
			return 0x0085;
		}

		@Override
		public String errorMessage() {
			return "Busy.";
		}
	},

	TEMP_FAILURE {
		@Override
		public short fieldValue() {
			return 0x0086;
		}

		@Override
		public String errorMessage() {
			return "Temporary failure .";
		}
	},

	AUTH_REQUIRED {
		@Override
		public short fieldValue() {
			return 0x20;
		}

		@Override
		public String errorMessage() {
			return "Authentication required or not successful";
		}
	},
	FUTHER_AUTH_REQUIRED {
		@Override
		public short fieldValue() {
			return 0x21;
		}

		@Override
		public String errorMessage() {
			return "Further authentication steps required. ";
		}
	};
	abstract short fieldValue();

	/**
	 * Get status from short value
	 * 
	 * @param value
	 * @return
	 */
	public static ResponseStatus parseShort(short value) {
		switch (value) {
		case 0x0000:
			return NO_ERROR;
		case 0x0001:
			return KEY_NOT_FOUND;
		case 0x0002:
			return KEY_EXISTS;
		case 0x0003:
			return VALUE_TOO_BIG;
		case 0x0004:
			return INVALID_ARGUMENTS;
		case 0x0005:
			return ITEM_NOT_STORED;
		case 0x0006:
			return INC_DEC_NON_NUM;
		case 0x0007:
			return BELONGS_TO_ANOTHER_SRV;
		case 0x0008:
			return AUTH_ERROR;
		case 0x0009:
			return AUTH_CONTINUE;
		case 0x0081:
			return UNKNOWN_COMMAND;
		case 0x0082:
			return OUT_OF_MEMORY;
		case 0x0083:
			return NOT_SUPPORTED;
		case 0x0084:
			return INTERNAL_ERROR;
		case 0x0085:
			return BUSY;
		case 0x0086:
			return TEMP_FAILURE;
		case 0x20:
			return AUTH_REQUIRED;
		case 0x21:
			return FUTHER_AUTH_REQUIRED;
		default:
			throw new IllegalArgumentException("Unknow Response status:"
					+ value);
		}
	}

	/**
	 * The status error message
	 * 
	 * @return
	 */
	abstract String errorMessage();
}
