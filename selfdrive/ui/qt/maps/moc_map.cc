/****************************************************************************
** Meta object code from reading C++ file 'map.h'
**
** Created by: The Qt Meta Object Compiler version 67 (Qt 5.12.8)
**
** WARNING! All changes made in this file will be lost!
*****************************************************************************/

#include "map.h"
#include <QtCore/qbytearray.h>
#include <QtCore/qmetatype.h>
#if !defined(Q_MOC_OUTPUT_REVISION)
#error "The header file 'map.h' doesn't include <QObject>."
#elif Q_MOC_OUTPUT_REVISION != 67
#error "This file was generated using the moc from 5.12.8. It"
#error "cannot be used with the include files from this version of Qt."
#error "(The moc has changed too much.)"
#endif

QT_BEGIN_MOC_NAMESPACE
QT_WARNING_PUSH
QT_WARNING_DISABLE_DEPRECATED
struct qt_meta_stringdata_MapInstructions_t {
    QByteArrayData data[7];
    char stringdata0[96];
};
#define QT_MOC_LITERAL(idx, ofs, len) \
    Q_STATIC_BYTE_ARRAY_DATA_HEADER_INITIALIZER_WITH_OFFSET(len, \
    qptrdiff(offsetof(qt_meta_stringdata_MapInstructions_t, stringdata0) + ofs \
        - idx * sizeof(QByteArrayData)) \
    )
static const qt_meta_stringdata_MapInstructions_t qt_meta_stringdata_MapInstructions = {
    {
QT_MOC_LITERAL(0, 0, 15), // "MapInstructions"
QT_MOC_LITERAL(1, 16, 14), // "updateDistance"
QT_MOC_LITERAL(2, 31, 0), // ""
QT_MOC_LITERAL(3, 32, 1), // "d"
QT_MOC_LITERAL(4, 34, 18), // "updateInstructions"
QT_MOC_LITERAL(5, 53, 30), // "cereal::NavInstruction::Reader"
QT_MOC_LITERAL(6, 84, 11) // "instruction"

    },
    "MapInstructions\0updateDistance\0\0d\0"
    "updateInstructions\0cereal::NavInstruction::Reader\0"
    "instruction"
};
#undef QT_MOC_LITERAL

static const uint qt_meta_data_MapInstructions[] = {

 // content:
       8,       // revision
       0,       // classname
       0,    0, // classinfo
       2,   14, // methods
       0,    0, // properties
       0,    0, // enums/sets
       0,    0, // constructors
       0,       // flags
       0,       // signalCount

 // slots: name, argc, parameters, tag, flags
       1,    1,   24,    2, 0x0a /* Public */,
       4,    1,   27,    2, 0x0a /* Public */,

 // slots: parameters
    QMetaType::Void, QMetaType::Float,    3,
    QMetaType::Void, 0x80000000 | 5,    6,

       0        // eod
};

void MapInstructions::qt_static_metacall(QObject *_o, QMetaObject::Call _c, int _id, void **_a)
{
    if (_c == QMetaObject::InvokeMetaMethod) {
        auto *_t = static_cast<MapInstructions *>(_o);
        Q_UNUSED(_t)
        switch (_id) {
        case 0: _t->updateDistance((*reinterpret_cast< float(*)>(_a[1]))); break;
        case 1: _t->updateInstructions((*reinterpret_cast< cereal::NavInstruction::Reader(*)>(_a[1]))); break;
        default: ;
        }
    }
}

QT_INIT_METAOBJECT const QMetaObject MapInstructions::staticMetaObject = { {
    &QWidget::staticMetaObject,
    qt_meta_stringdata_MapInstructions.data,
    qt_meta_data_MapInstructions,
    qt_static_metacall,
    nullptr,
    nullptr
} };


const QMetaObject *MapInstructions::metaObject() const
{
    return QObject::d_ptr->metaObject ? QObject::d_ptr->dynamicMetaObject() : &staticMetaObject;
}

void *MapInstructions::qt_metacast(const char *_clname)
{
    if (!_clname) return nullptr;
    if (!strcmp(_clname, qt_meta_stringdata_MapInstructions.stringdata0))
        return static_cast<void*>(this);
    return QWidget::qt_metacast(_clname);
}

int MapInstructions::qt_metacall(QMetaObject::Call _c, int _id, void **_a)
{
    _id = QWidget::qt_metacall(_c, _id, _a);
    if (_id < 0)
        return _id;
    if (_c == QMetaObject::InvokeMetaMethod) {
        if (_id < 2)
            qt_static_metacall(this, _c, _id, _a);
        _id -= 2;
    } else if (_c == QMetaObject::RegisterMethodArgumentMetaType) {
        if (_id < 2)
            *reinterpret_cast<int*>(_a[0]) = -1;
        _id -= 2;
    }
    return _id;
}
struct qt_meta_stringdata_MapETA_t {
    QByteArrayData data[6];
    char stringdata0[51];
};
#define QT_MOC_LITERAL(idx, ofs, len) \
    Q_STATIC_BYTE_ARRAY_DATA_HEADER_INITIALIZER_WITH_OFFSET(len, \
    qptrdiff(offsetof(qt_meta_stringdata_MapETA_t, stringdata0) + ofs \
        - idx * sizeof(QByteArrayData)) \
    )
static const qt_meta_stringdata_MapETA_t qt_meta_stringdata_MapETA = {
    {
QT_MOC_LITERAL(0, 0, 6), // "MapETA"
QT_MOC_LITERAL(1, 7, 9), // "updateETA"
QT_MOC_LITERAL(2, 17, 0), // ""
QT_MOC_LITERAL(3, 18, 7), // "seconds"
QT_MOC_LITERAL(4, 26, 15), // "seconds_typical"
QT_MOC_LITERAL(5, 42, 8) // "distance"

    },
    "MapETA\0updateETA\0\0seconds\0seconds_typical\0"
    "distance"
};
#undef QT_MOC_LITERAL

static const uint qt_meta_data_MapETA[] = {

 // content:
       8,       // revision
       0,       // classname
       0,    0, // classinfo
       1,   14, // methods
       0,    0, // properties
       0,    0, // enums/sets
       0,    0, // constructors
       0,       // flags
       0,       // signalCount

 // slots: name, argc, parameters, tag, flags
       1,    3,   19,    2, 0x0a /* Public */,

 // slots: parameters
    QMetaType::Void, QMetaType::Float, QMetaType::Float, QMetaType::Float,    3,    4,    5,

       0        // eod
};

void MapETA::qt_static_metacall(QObject *_o, QMetaObject::Call _c, int _id, void **_a)
{
    if (_c == QMetaObject::InvokeMetaMethod) {
        auto *_t = static_cast<MapETA *>(_o);
        Q_UNUSED(_t)
        switch (_id) {
        case 0: _t->updateETA((*reinterpret_cast< float(*)>(_a[1])),(*reinterpret_cast< float(*)>(_a[2])),(*reinterpret_cast< float(*)>(_a[3]))); break;
        default: ;
        }
    }
}

QT_INIT_METAOBJECT const QMetaObject MapETA::staticMetaObject = { {
    &QWidget::staticMetaObject,
    qt_meta_stringdata_MapETA.data,
    qt_meta_data_MapETA,
    qt_static_metacall,
    nullptr,
    nullptr
} };


const QMetaObject *MapETA::metaObject() const
{
    return QObject::d_ptr->metaObject ? QObject::d_ptr->dynamicMetaObject() : &staticMetaObject;
}

void *MapETA::qt_metacast(const char *_clname)
{
    if (!_clname) return nullptr;
    if (!strcmp(_clname, qt_meta_stringdata_MapETA.stringdata0))
        return static_cast<void*>(this);
    return QWidget::qt_metacast(_clname);
}

int MapETA::qt_metacall(QMetaObject::Call _c, int _id, void **_a)
{
    _id = QWidget::qt_metacall(_c, _id, _a);
    if (_id < 0)
        return _id;
    if (_c == QMetaObject::InvokeMetaMethod) {
        if (_id < 1)
            qt_static_metacall(this, _c, _id, _a);
        _id -= 1;
    } else if (_c == QMetaObject::RegisterMethodArgumentMetaType) {
        if (_id < 1)
            *reinterpret_cast<int*>(_a[0]) = -1;
        _id -= 1;
    }
    return _id;
}
struct qt_meta_stringdata_MapWindow_t {
    QByteArrayData data[15];
    char stringdata0[182];
};
#define QT_MOC_LITERAL(idx, ofs, len) \
    Q_STATIC_BYTE_ARRAY_DATA_HEADER_INITIALIZER_WITH_OFFSET(len, \
    qptrdiff(offsetof(qt_meta_stringdata_MapWindow_t, stringdata0) + ofs \
        - idx * sizeof(QByteArrayData)) \
    )
static const qt_meta_stringdata_MapWindow_t qt_meta_stringdata_MapWindow = {
    {
QT_MOC_LITERAL(0, 0, 9), // "MapWindow"
QT_MOC_LITERAL(1, 10, 15), // "distanceChanged"
QT_MOC_LITERAL(2, 26, 0), // ""
QT_MOC_LITERAL(3, 27, 8), // "distance"
QT_MOC_LITERAL(4, 36, 19), // "instructionsChanged"
QT_MOC_LITERAL(5, 56, 30), // "cereal::NavInstruction::Reader"
QT_MOC_LITERAL(6, 87, 11), // "instruction"
QT_MOC_LITERAL(7, 99, 10), // "ETAChanged"
QT_MOC_LITERAL(8, 110, 7), // "seconds"
QT_MOC_LITERAL(9, 118, 15), // "seconds_typical"
QT_MOC_LITERAL(10, 134, 11), // "updateState"
QT_MOC_LITERAL(11, 146, 7), // "UIState"
QT_MOC_LITERAL(12, 154, 1), // "s"
QT_MOC_LITERAL(13, 156, 17), // "offroadTransition"
QT_MOC_LITERAL(14, 174, 7) // "offroad"

    },
    "MapWindow\0distanceChanged\0\0distance\0"
    "instructionsChanged\0cereal::NavInstruction::Reader\0"
    "instruction\0ETAChanged\0seconds\0"
    "seconds_typical\0updateState\0UIState\0"
    "s\0offroadTransition\0offroad"
};
#undef QT_MOC_LITERAL

static const uint qt_meta_data_MapWindow[] = {

 // content:
       8,       // revision
       0,       // classname
       0,    0, // classinfo
       5,   14, // methods
       0,    0, // properties
       0,    0, // enums/sets
       0,    0, // constructors
       0,       // flags
       3,       // signalCount

 // signals: name, argc, parameters, tag, flags
       1,    1,   39,    2, 0x06 /* Public */,
       4,    1,   42,    2, 0x06 /* Public */,
       7,    3,   45,    2, 0x06 /* Public */,

 // slots: name, argc, parameters, tag, flags
      10,    1,   52,    2, 0x08 /* Private */,
      13,    1,   55,    2, 0x0a /* Public */,

 // signals: parameters
    QMetaType::Void, QMetaType::Float,    3,
    QMetaType::Void, 0x80000000 | 5,    6,
    QMetaType::Void, QMetaType::Float, QMetaType::Float, QMetaType::Float,    8,    9,    3,

 // slots: parameters
    QMetaType::Void, 0x80000000 | 11,   12,
    QMetaType::Void, QMetaType::Bool,   14,

       0        // eod
};

void MapWindow::qt_static_metacall(QObject *_o, QMetaObject::Call _c, int _id, void **_a)
{
    if (_c == QMetaObject::InvokeMetaMethod) {
        auto *_t = static_cast<MapWindow *>(_o);
        Q_UNUSED(_t)
        switch (_id) {
        case 0: _t->distanceChanged((*reinterpret_cast< float(*)>(_a[1]))); break;
        case 1: _t->instructionsChanged((*reinterpret_cast< cereal::NavInstruction::Reader(*)>(_a[1]))); break;
        case 2: _t->ETAChanged((*reinterpret_cast< float(*)>(_a[1])),(*reinterpret_cast< float(*)>(_a[2])),(*reinterpret_cast< float(*)>(_a[3]))); break;
        case 3: _t->updateState((*reinterpret_cast< const UIState(*)>(_a[1]))); break;
        case 4: _t->offroadTransition((*reinterpret_cast< bool(*)>(_a[1]))); break;
        default: ;
        }
    } else if (_c == QMetaObject::IndexOfMethod) {
        int *result = reinterpret_cast<int *>(_a[0]);
        {
            using _t = void (MapWindow::*)(float );
            if (*reinterpret_cast<_t *>(_a[1]) == static_cast<_t>(&MapWindow::distanceChanged)) {
                *result = 0;
                return;
            }
        }
        {
            using _t = void (MapWindow::*)(cereal::NavInstruction::Reader );
            if (*reinterpret_cast<_t *>(_a[1]) == static_cast<_t>(&MapWindow::instructionsChanged)) {
                *result = 1;
                return;
            }
        }
        {
            using _t = void (MapWindow::*)(float , float , float );
            if (*reinterpret_cast<_t *>(_a[1]) == static_cast<_t>(&MapWindow::ETAChanged)) {
                *result = 2;
                return;
            }
        }
    }
}

QT_INIT_METAOBJECT const QMetaObject MapWindow::staticMetaObject = { {
    &QOpenGLWidget::staticMetaObject,
    qt_meta_stringdata_MapWindow.data,
    qt_meta_data_MapWindow,
    qt_static_metacall,
    nullptr,
    nullptr
} };


const QMetaObject *MapWindow::metaObject() const
{
    return QObject::d_ptr->metaObject ? QObject::d_ptr->dynamicMetaObject() : &staticMetaObject;
}

void *MapWindow::qt_metacast(const char *_clname)
{
    if (!_clname) return nullptr;
    if (!strcmp(_clname, qt_meta_stringdata_MapWindow.stringdata0))
        return static_cast<void*>(this);
    return QOpenGLWidget::qt_metacast(_clname);
}

int MapWindow::qt_metacall(QMetaObject::Call _c, int _id, void **_a)
{
    _id = QOpenGLWidget::qt_metacall(_c, _id, _a);
    if (_id < 0)
        return _id;
    if (_c == QMetaObject::InvokeMetaMethod) {
        if (_id < 5)
            qt_static_metacall(this, _c, _id, _a);
        _id -= 5;
    } else if (_c == QMetaObject::RegisterMethodArgumentMetaType) {
        if (_id < 5)
            *reinterpret_cast<int*>(_a[0]) = -1;
        _id -= 5;
    }
    return _id;
}

// SIGNAL 0
void MapWindow::distanceChanged(float _t1)
{
    void *_a[] = { nullptr, const_cast<void*>(reinterpret_cast<const void*>(&_t1)) };
    QMetaObject::activate(this, &staticMetaObject, 0, _a);
}

// SIGNAL 1
void MapWindow::instructionsChanged(cereal::NavInstruction::Reader _t1)
{
    void *_a[] = { nullptr, const_cast<void*>(reinterpret_cast<const void*>(&_t1)) };
    QMetaObject::activate(this, &staticMetaObject, 1, _a);
}

// SIGNAL 2
void MapWindow::ETAChanged(float _t1, float _t2, float _t3)
{
    void *_a[] = { nullptr, const_cast<void*>(reinterpret_cast<const void*>(&_t1)), const_cast<void*>(reinterpret_cast<const void*>(&_t2)), const_cast<void*>(reinterpret_cast<const void*>(&_t3)) };
    QMetaObject::activate(this, &staticMetaObject, 2, _a);
}
QT_WARNING_POP
QT_END_MOC_NAMESPACE
