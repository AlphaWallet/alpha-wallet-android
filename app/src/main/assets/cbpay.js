"use strict";
function _arrayLikeToArray(arr, len) {
    if (len == null || len > arr.length) len = arr.length;
    for(var i = 0, arr2 = new Array(len); i < len; i++)arr2[i] = arr[i];
    return arr2;
}
function _arrayWithHoles(arr) {
    if (Array.isArray(arr)) return arr;
}
function _arrayWithoutHoles(arr) {
    if (Array.isArray(arr)) return _arrayLikeToArray(arr);
}
function _classCallCheck(instance, Constructor) {
    if (!(instance instanceof Constructor)) {
        throw new TypeError("Cannot call a class as a function");
    }
}
function _defineProperty(obj, key, value) {
    if (key in obj) {
        Object.defineProperty(obj, key, {
            value: value,
            enumerable: true,
            configurable: true,
            writable: true
        });
    } else {
        obj[key] = value;
    }
    return obj;
}
function _iterableToArray(iter) {
    if (typeof Symbol !== "undefined" && iter[Symbol.iterator] != null || iter["@@iterator"] != null) return Array.from(iter);
}
function _iterableToArrayLimit(arr, i) {
    var _i = arr == null ? null : typeof Symbol !== "undefined" && arr[Symbol.iterator] || arr["@@iterator"];
    if (_i == null) return;
    var _arr = [];
    var _n = true;
    var _d = false;
    var _s, _e;
    try {
        for(_i = _i.call(arr); !(_n = (_s = _i.next()).done); _n = true){
            _arr.push(_s.value);
            if (i && _arr.length === i) break;
        }
    } catch (err) {
        _d = true;
        _e = err;
    } finally{
        try {
            if (!_n && _i["return"] != null) _i["return"]();
        } finally{
            if (_d) throw _e;
        }
    }
    return _arr;
}
function _nonIterableRest() {
    throw new TypeError("Invalid attempt to destructure non-iterable instance.\\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.");
}
function _nonIterableSpread() {
    throw new TypeError("Invalid attempt to spread non-iterable instance.\\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.");
}
function _objectSpread(target) {
    for(var i = 1; i < arguments.length; i++){
        var source = arguments[i] != null ? arguments[i] : {};
        var ownKeys = Object.keys(source);
        if (typeof Object.getOwnPropertySymbols === "function") {
            ownKeys = ownKeys.concat(Object.getOwnPropertySymbols(source).filter(function(sym) {
                return Object.getOwnPropertyDescriptor(source, sym).enumerable;
            }));
        }
        ownKeys.forEach(function(key) {
            _defineProperty(target, key, source[key]);
        });
    }
    return target;
}
function _objectWithoutProperties(source, excluded) {
    if (source == null) return {};
    var target = _objectWithoutPropertiesLoose(source, excluded);
    var key, i;
    if (Object.getOwnPropertySymbols) {
        var sourceSymbolKeys = Object.getOwnPropertySymbols(source);
        for(i = 0; i < sourceSymbolKeys.length; i++){
            key = sourceSymbolKeys[i];
            if (excluded.indexOf(key) >= 0) continue;
            if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue;
            target[key] = source[key];
        }
    }
    return target;
}
function _objectWithoutPropertiesLoose(source, excluded) {
    if (source == null) return {};
    var target = {};
    var sourceKeys = Object.keys(source);
    var key, i;
    for(i = 0; i < sourceKeys.length; i++){
        key = sourceKeys[i];
        if (excluded.indexOf(key) >= 0) continue;
        target[key] = source[key];
    }
    return target;
}
function _slicedToArray(arr, i) {
    return _arrayWithHoles(arr) || _iterableToArrayLimit(arr, i) || _unsupportedIterableToArray(arr, i) || _nonIterableRest();
}
function _toConsumableArray(arr) {
    return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _unsupportedIterableToArray(arr) || _nonIterableSpread();
}
function _unsupportedIterableToArray(o, minLen) {
    if (!o) return;
    if (typeof o === "string") return _arrayLikeToArray(o, minLen);
    var n = Object.prototype.toString.call(o).slice(8, -1);
    if (n === "Object" && o.constructor) n = o.constructor.name;
    if (n === "Map" || n === "Set") return Array.from(n);
    if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen);
}
Object.defineProperty(exports, "__esModule", {
    value: true
});
function _optionalChain(ops) {
    var lastAccessLHS = undefined;
    var value = ops[0];
    var i = 1;
    while(i < ops.length){
        var op = ops[i];
        var fn = ops[i + 1];
        i += 2;
        if ((op === "optionalAccess" || op === "optionalCall") && value == null) {
            return undefined;
        }
        if (op === "access" || op === "optionalAccess") {
            lastAccessLHS = value;
            value = fn(value);
        } else if (op === "call" || op === "optionalCall") {
            var _value;
            value = fn(function() {
                for(var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++){
                    args[_key] = arguments[_key];
                }
                return (_value = value).call.apply(_value, [
                    lastAccessLHS
                ].concat(_toConsumableArray(args)));
            });
            lastAccessLHS = undefined;
        }
    }
    return value;
}
var _chunkBM7N7EVAjs = require("./chunk-BM7N7EVA.js");
// src/config.ts
var DEFAULT_HOST = "https://pay.coinbase.com";
// src/utils/parseDestinationWallets.ts
function parseDestinationWallets(wallets) {
    var map = wallets.reduce(function(prev, param) {
        var address = param.address, blockchains = param.blockchains, assets = param.assets;
        prev[address] = (prev[address] || []).concat(blockchains || []).concat(assets || []);
        return prev;
    }, {});
    Object.entries(map).forEach(function(param) {
        var _param = _slicedToArray(param, 2), address = _param[0], blockchains = _param[1];
        map[address] = _toConsumableArray(new Set(blockchains));
    });
    return map;
}
_chunkBM7N7EVAjs.__name.call(void 0, parseDestinationWallets, "parseDestinationWallets");
// src/onramp/generateOnRampURL.ts
var generateOnRampURL = /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function(param) {
    var appId = param.appId, _host = param.host, host = _host === void 0 ? DEFAULT_HOST : _host, destinationWallets = param.destinationWallets, presetFiatAmount = param.presetFiatAmount, presetCryptoAmount = param.presetCryptoAmount;
    var url = new URL(host);
    url.pathname = "/buy/select-asset";
    url.searchParams.append("appId", appId);
    url.searchParams.append("destinationWallets", JSON.stringify(parseDestinationWallets(destinationWallets)));
    if (presetFiatAmount) {
        url.searchParams.append("presetFiatAmount", _optionalChain([
            presetFiatAmount,
            "optionalAccess",
            function(_) {
                return _.toString;
            },
            "call",
            function(_2) {
                return _2();
            }
        ]));
    }
    if (presetCryptoAmount) {
        url.searchParams.append("presetCryptoAmount", _optionalChain([
            presetCryptoAmount,
            "optionalAccess",
            function(_3) {
                return _3.toString;
            },
            "call",
            function(_4) {
                return _4();
            }
        ]));
    }
    return url.toString();
}, "generateOnRampURL");
// src/utils/createEmbeddedContent.ts
var EMBEDDED_IFRAME_ID = "cbpay-embedded-onramp";
var createEmbeddedContent = /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function(param) {
    var url = param.url, _width = param.width, width = _width === void 0 ? "100%" : _width, _height = param.height, height = _height === void 0 ? "100%" : _height, _position = param.position, position = _position === void 0 ? "fixed" : _position, _top = param.top, top = _top === void 0 ? "0px" : _top;
    var iframe = document.createElement("iframe");
    iframe.style.border = "unset";
    iframe.style.borderWidth = "0";
    iframe.style.width = width.toString();
    iframe.style.height = height.toString();
    iframe.style.position = position;
    iframe.style.top = top;
    iframe.id = EMBEDDED_IFRAME_ID;
    iframe.src = url;
    return iframe;
}, "createEmbeddedContent");
// src/utils/CoinbasePixel.ts
var PIXEL_PATH = "/embed";
var PIXEL_ID = "coinbase-sdk-connect";
var PopupSizes = {
    signin: {
        width: 460,
        height: 730
    },
    widget: {
        width: 430,
        height: 600
    }
};
var CoinbasePixel = function _class(param1) {
    var _host = param1.host, host = _host === void 0 ? DEFAULT_HOST : _host, appId = param1.appId, appParams1 = param1.appParams, onReady = param1.onReady;
    var _this3 = this;
    _classCallCheck(this, _class);
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "nonce", "");
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "eventStreamListeners", {});
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "unsubs", []);
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "isReady", false);
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "isLoggedIn", false);
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "openExperience", /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function(options) {
        var _this2 = _this3;
        if (!_this3.isReady) {
            var _this1 = _this3;
            _this3.onMessage("on_app_params_nonce", {
                onMessage: function() {
                    _this1.openExperience(options);
                }
            });
            return;
        }
        var path = options.path, experienceLoggedIn = options.experienceLoggedIn, experienceLoggedOut = options.experienceLoggedOut, embeddedContentStyles = options.embeddedContentStyles, onExit = options.onExit, onSuccess = options.onSuccess, onEvent = options.onEvent;
        var widgetUrl = new URL("".concat(_this3.host).concat(path));
        widgetUrl.searchParams.append("appId", _this3.appId);
        widgetUrl.searchParams.append("type", "secure_standalone");
        var experience = _this3.isLoggedIn ? experienceLoggedIn : experienceLoggedOut || experienceLoggedIn;
        _this3.setupExperienceListeners({
            onExit: onExit,
            onSuccess: onSuccess,
            onEvent: onEvent
        });
        _this3.sendAppParams(_this3.appParams, function() {
            widgetUrl.searchParams.append("nonce", _this2.nonce);
            var url = widgetUrl.toString();
            _this2.nonce = "";
            if (experience === "embedded") {
                var openEmbeddedExperience = /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function() {
                    var embedded = createEmbeddedContent(_objectSpread({
                        url: url
                    }, embeddedContentStyles));
                    if (_optionalChain([
                        embeddedContentStyles,
                        "optionalAccess",
                        function(_5) {
                            return _5.target;
                        }
                    ])) {
                        _optionalChain([
                            document,
                            "access",
                            function(_6) {
                                return _6.querySelector;
                            },
                            "call",
                            function(_7) {
                                return _7(_optionalChain([
                                    embeddedContentStyles,
                                    "optionalAccess",
                                    function(_8) {
                                        return _8.target;
                                    }
                                ]));
                            },
                            "optionalAccess",
                            function(_9) {
                                return _9.appendChild;
                            },
                            "call",
                            function(_10) {
                                return _10(embedded);
                            }
                        ]);
                    } else {
                        document.body.appendChild(embedded);
                    }
                }, "openEmbeddedExperience");
                if (!_this2.isLoggedIn) {
                    _this2.startDirectSignin(openEmbeddedExperience);
                } else {
                    openEmbeddedExperience();
                }
            } else if (experience === "popup" && _optionalChain([
                window,
                "access",
                function(_11) {
                    return _11.chrome;
                },
                "optionalAccess",
                function(_12) {
                    return _12.windows;
                },
                "optionalAccess",
                function(_13) {
                    return _13.create;
                }
            ])) {
                var _this = _this2;
                void window.chrome.windows.create({
                    url: url,
                    setSelfAsOpener: true,
                    type: "popup",
                    focused: true,
                    width: PopupSizes.signin.width,
                    height: PopupSizes.signin.height,
                    left: window.screenLeft - PopupSizes.signin.width - 10,
                    top: window.screenTop
                }, function(winRef) {
                    _this.addEventStreamListener("open", function() {
                        if (_optionalChain([
                            winRef,
                            "optionalAccess",
                            function(_14) {
                                return _14.id;
                            }
                        ])) {
                            chrome.windows.update(winRef.id, {
                                width: PopupSizes.widget.width,
                                height: PopupSizes.widget.height,
                                left: window.screenLeft - PopupSizes.widget.width - 10,
                                top: window.screenTop
                            });
                        }
                    });
                });
            } else if (experience === "new_tab" && _optionalChain([
                window,
                "access",
                function(_15) {
                    return _15.chrome;
                },
                "optionalAccess",
                function(_16) {
                    return _16.tabs;
                },
                "optionalAccess",
                function(_17) {
                    return _17.create;
                }
            ])) {
                void window.chrome.tabs.create({
                    url: url
                });
            } else {
                openWindow(url, experience);
            }
        });
    }, "openExperience"));
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "endExperience", /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function() {
        _optionalChain([
            document,
            "access",
            function(_18) {
                return _18.getElementById;
            },
            "call",
            function(_19) {
                return _19(EMBEDDED_IFRAME_ID);
            },
            "optionalAccess",
            function(_20) {
                return _20.remove;
            },
            "call",
            function(_21) {
                return _21();
            }
        ]);
    }, "endExperience"));
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "destroy", /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function() {
        _optionalChain([
            document,
            "access",
            function(_22) {
                return _22.getElementById;
            },
            "call",
            function(_23) {
                return _23(PIXEL_ID);
            },
            "optionalAccess",
            function(_24) {
                return _24.remove;
            },
            "call",
            function(_25) {
                return _25();
            }
        ]);
        _this3.unsubs.forEach(function(unsub) {
            return unsub();
        });
    }, "destroy"));
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "setupListeners", /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function() {
        var _this = _this3;
        _this3.onMessage("pixel_ready", {
            shouldUnsubscribe: false,
            onMessage: function(data) {
                _this.isLoggedIn = !!_optionalChain([
                    data,
                    "optionalAccess",
                    function(_26) {
                        return _26.isLoggedIn;
                    }
                ]);
                _this.sendAppParams(_this.appParams);
            }
        });
        _this3.onMessage("on_app_params_nonce", {
            shouldUnsubscribe: true,
            onMessage: function() {
                _this.isReady = true;
                _optionalChain([
                    _this,
                    "access",
                    function(_27) {
                        return _27.onReadyCallback;
                    },
                    "optionalCall",
                    function(_28) {
                        return _28();
                    }
                ]);
            }
        });
        _this3.onMessage("on_app_params_nonce", {
            shouldUnsubscribe: false,
            onMessage: function(data) {
                _this.nonce = _optionalChain([
                    data,
                    "optionalAccess",
                    function(_29) {
                        return _29.nonce;
                    }
                ]) || "";
            }
        });
    }, "setupListeners"));
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "embedPixel", /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function() {
        var _this = _this3;
        _optionalChain([
            document,
            "access",
            function(_30) {
                return _30.getElementById;
            },
            "call",
            function(_31) {
                return _31(PIXEL_ID);
            },
            "optionalAccess",
            function(_32) {
                return _32.remove;
            },
            "call",
            function(_33) {
                return _33();
            }
        ]);
        var pixel = createPixel({
            host: _this3.host,
            appId: _this3.appId
        });
        pixel.onerror = function() {
            _optionalChain([
                _this,
                "access",
                function(_34) {
                    return _34.onReadyCallback;
                },
                "optionalCall",
                function(_35) {
                    return _35(new Error("Failed to initialize app"));
                }
            ]);
        };
        _this3.pixelIframe = pixel;
        document.body.appendChild(pixel);
    }, "embedPixel"));
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "sendAppParams", /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function(appParams, callback) {
        if (_this3.nonce) {
            _optionalChain([
                callback,
                "optionalCall",
                function(_36) {
                    return _36();
                }
            ]);
        } else if (_this3.pixelIframe) {
            _chunkBM7N7EVAjs.broadcastPostMessage.call(void 0, _this3.pixelIframe.contentWindow, "app_params", {
                data: appParams
            });
            _this3.onMessage("on_app_params_nonce", {
                onMessage: function() {
                    return _optionalChain([
                        callback,
                        "optionalCall",
                        function(_37) {
                            return _37();
                        }
                    ]);
                }
            });
        }
    }, "sendAppParams"));
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "setupExperienceListeners", /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function(param) {
        var onSuccess = param.onSuccess, onExit = param.onExit, onEvent = param.onEvent;
        if (onEvent) {
            var _this = _this3;
            _this3.onMessage("event", {
                shouldUnsubscribe: false,
                onMessage: function(data) {
                    var metadata = data;
                    _optionalChain([
                        _this,
                        "access",
                        function(_38) {
                            return _38.eventStreamListeners;
                        },
                        "access",
                        function(_39) {
                            return _39[metadata.eventName];
                        },
                        "optionalAccess",
                        function(_40) {
                            return _40.forEach;
                        },
                        "call",
                        function(_41) {
                            return _41(function(cb) {
                                return _optionalChain([
                                    cb,
                                    "optionalCall",
                                    function(_42) {
                                        return _42();
                                    }
                                ]);
                            });
                        }
                    ]);
                    if (metadata.eventName === "success") {
                        _optionalChain([
                            onSuccess,
                            "optionalCall",
                            function(_43) {
                                return _43();
                            }
                        ]);
                    }
                    if (metadata.eventName === "exit") {
                        _optionalChain([
                            onExit,
                            "optionalCall",
                            function(_44) {
                                return _44(metadata.error);
                            }
                        ]);
                    }
                    onEvent(data);
                }
            });
        }
    }, "setupExperienceListeners"));
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "startDirectSignin", /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function(callback) {
        var queryParams = new URLSearchParams();
        queryParams.set("appId", _this3.appId);
        queryParams.set("type", "direct");
        var directSigninUrl = "".concat(_this3.host, "/signin?").concat(queryParams.toString());
        var signinWinRef = openWindow(directSigninUrl, "popup");
        _this3.onMessage("signin_success", {
            onMessage: function() {
                _optionalChain([
                    signinWinRef,
                    "optionalAccess",
                    function(_45) {
                        return _45.close;
                    },
                    "call",
                    function(_46) {
                        return _46();
                    }
                ]);
                callback();
            }
        });
    }, "startDirectSignin"));
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "addEventStreamListener", /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function(name, cb) {
        if (_this3.eventStreamListeners[name]) {
            _optionalChain([
                _this3,
                "access",
                function(_47) {
                    return _47.eventStreamListeners;
                },
                "access",
                function(_48) {
                    return _48[name];
                },
                "optionalAccess",
                function(_49) {
                    return _49.push;
                },
                "call",
                function(_50) {
                    return _50(cb);
                }
            ]);
        } else {
            _this3.eventStreamListeners[name] = [
                cb
            ];
        }
    }, "addEventStreamListener"));
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "onMessage", /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function() {
        for(var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++){
            args[_key] = arguments[_key];
        }
        _this3.unsubs.push(_chunkBM7N7EVAjs.onBroadcastedPostMessage.call(void 0, args[0], _objectSpread({
            allowedOrigin: _this3.host
        }, args[1])));
    }, "onMessage"));
    this.host = host;
    this.appId = appId;
    this.appParams = appParams1;
    this.onReadyCallback = onReady;
    this.setupListeners();
    this.embedPixel();
};
_chunkBM7N7EVAjs.__name.call(void 0, CoinbasePixel, "CoinbasePixel");
function createPixel(param) {
    var host = param.host, appId = param.appId;
    var pixel = document.createElement("iframe");
    pixel.style.border = "unset";
    pixel.style.borderWidth = "0";
    pixel.style.width = "0";
    pixel.style.height = "0";
    pixel.style.height = "0";
    pixel.id = PIXEL_ID;
    var url = new URL("".concat(host).concat(PIXEL_PATH));
    url.searchParams.append("appId", appId);
    pixel.src = url.toString();
    return pixel;
}
_chunkBM7N7EVAjs.__name.call(void 0, createPixel, "createPixel");
function openWindow(url, experience) {
    return window.open(url, "Coinbase", experience === "popup" ? "toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=no, resizable=no, copyhistory=no, height=".concat(PopupSizes.signin.height, ",width=").concat(PopupSizes.signin.width) : void 0);
}
_chunkBM7N7EVAjs.__name.call(void 0, openWindow, "openWindow");
// src/utils/CBPayInstance.ts
var widgetRoutes = {
    buy: "/buy",
    checkout: "/checkout"
};
var CBPayInstance = function _class(options) {
    var _this = this;
    _classCallCheck(this, _class);
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "open", /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function() {
        var _this4 = _this;
        var _options = _this.options, widget = _options.widget, experienceLoggedIn = _options.experienceLoggedIn, experienceLoggedOut = _options.experienceLoggedOut, embeddedContentStyles = _options.embeddedContentStyles, onExit = _options.onExit, onSuccess = _options.onSuccess, onEvent = _options.onEvent, closeOnSuccess = _options.closeOnSuccess, closeOnExit = _options.closeOnExit;
        _this.pixel.openExperience({
            path: widgetRoutes[widget],
            experienceLoggedIn: experienceLoggedIn,
            experienceLoggedOut: experienceLoggedOut,
            embeddedContentStyles: embeddedContentStyles,
            onExit: function() {
                _optionalChain([
                    onExit,
                    "optionalCall",
                    function(_51) {
                        return _51();
                    }
                ]);
                if (closeOnExit) {
                    _this4.pixel.endExperience();
                }
            },
            onSuccess: function() {
                _optionalChain([
                    onSuccess,
                    "optionalCall",
                    function(_52) {
                        return _52();
                    }
                ]);
                if (closeOnSuccess) {
                    _this4.pixel.endExperience();
                }
            },
            onEvent: onEvent
        });
    }, "open"));
    _chunkBM7N7EVAjs.__publicField.call(void 0, this, "destroy", /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function() {
        _this.pixel.destroy();
    }, "destroy"));
    var appParams = _objectSpread({
        widget: options.widget
    }, options.appParams);
    this.options = options;
    this.pixel = new CoinbasePixel({
        host: options.host,
        appId: options.appId,
        appParams: appParams,
        onReady: options.onReady
    });
    if (options.target) {
        var targetElement = document.querySelector(options.target);
        if (targetElement) {
            targetElement.addEventListener("click", this.open);
        }
    }
};
_chunkBM7N7EVAjs.__name.call(void 0, CBPayInstance, "CBPayInstance");
// src/onramp/initOnRamp.ts
var initOnRamp = /* @__PURE__ */ _chunkBM7N7EVAjs.__name.call(void 0, function(_param) {
    var _experienceLoggedIn = _param.experienceLoggedIn, experienceLoggedIn = _experienceLoggedIn === void 0 ? "embedded" : _experienceLoggedIn, widgetParameters = _param.widgetParameters, options = _objectWithoutProperties(_param, [
        "experienceLoggedIn",
        "widgetParameters"
    ]);
    var instance = new CBPayInstance(_objectSpread({}, options, {
        widget: "buy",
        experienceLoggedIn: experienceLoggedIn,
        appParams: widgetParameters
    }));
    return instance;
}, "initOnRamp");
exports.generateOnRampURL = generateOnRampURL;
exports.initOnRamp = initOnRamp;
//# sourceMappingURL=index.js.map