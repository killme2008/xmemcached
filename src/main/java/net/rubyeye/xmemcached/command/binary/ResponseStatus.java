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
	UNKNOWN_COMMAND {
		@Override
		public short fieldValue() {
			return 0x0081;
		}

		@Override
		public String errorMessage() {
			return "Unknown command error.";
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
		case 0x0081:
			return UNKNOWN_COMMAND;
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
